package no.dcat.controller;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import no.dcat.model.Catalog;
import no.dcat.model.Dataset;
import no.dcat.shared.DataTheme;
import no.dcat.shared.SkosCode;
import no.dcat.model.exceptions.CatalogNotFoundException;
import no.dcat.model.exceptions.CodesImportException;
import no.dcat.model.exceptions.DatasetNotFoundException;
import no.dcat.model.exceptions.ErrorResponse;
import no.dcat.service.CatalogRepository;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(value = "/catalogs/{id}/import")
public class ImportController {

    private static Logger logger = LoggerFactory.getLogger(ImportController.class);

    protected final CatalogController catalogController;

    protected final DatasetController datasetController;

    protected final CatalogRepository catalogRepository;

    @Value("${application.themesServiceUrl}")
    private String  THEMES_SERVICE_URL = "http://error.themes.service.url.not.set";

    private final Map<String,Map<String,SkosCode>> allCodes = new HashMap<>();
    final Map<String,DataTheme> allThemes = new HashMap<>();
    private final static Model owlSchema = FileManager.get().loadModel("frames/schema.ttl");

    @Autowired
    public ImportController(CatalogController catalogController, DatasetController datasetController, CatalogRepository catalogRepository) {
        this.catalogController = catalogController;
        this.datasetController = datasetController;
        this.catalogRepository = catalogRepository;
    }

    @PreAuthorize("hasPermission(#catalogId, 'write')")
    @CrossOrigin
    @RequestMapping(value = "",
            method = POST,
            produces = APPLICATION_JSON_UTF8_VALUE)
    public HttpEntity<Catalog> importCatalog(
            @PathVariable(value = "id") String catalogId,
            @RequestBody String url) throws DatasetNotFoundException, CatalogNotFoundException, IOException {
        logger.info("import requested for {} starts", url);
        Catalog catalog;

        catalog = importDatasets(catalogId, new URL(url));

        logger.info("import request for {} finished", url);
        return new ResponseEntity<>(catalog, HttpStatus.OK);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception ex) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode(HttpStatus.BAD_REQUEST.value());
        error.setMessage(ex.getMessage());

        return new ResponseEntity<ErrorResponse>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {CatalogNotFoundException.class, DatasetNotFoundException.class })
    public ResponseEntity<ErrorResponse> notFoundException(Exception ex) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode(HttpStatus.NOT_FOUND.value());
        error.setMessage(ex.getMessage());

        return new ResponseEntity<ErrorResponse>(error, HttpStatus.NOT_FOUND);
    }

    private Catalog importDatasets(String catalogId, URL url) throws IOException, CatalogNotFoundException, DatasetNotFoundException {
        if(!(url.getProtocol().equals("http") || url.getProtocol().equals("https"))){
            throw new MalformedURLException("Supports only http and https");
        }

        Model model = null;

        try {
            model = FileManager.get().loadModel(url.toString());
        } catch (NullPointerException e) {
            throw new IOException(String.format("Cannot open import url %s",url));
        }

        Catalog existingCatalog = catalogRepository.findOne(catalogId);

        if (existingCatalog == null) {
            throw new CatalogNotFoundException(String.format("Catalog %s does not exist in registration database", catalogId));
        }

        Catalog catalogToImportTo = parseCatalog(model, existingCatalog, catalogId);

        if (catalogToImportTo == null) {
            throw new CatalogNotFoundException(String.format("Catalog %s is not found in imported data", catalogId));
        }

        List<Dataset> importedDatasets = parseAndSaveDatasets(model, catalogToImportTo, catalogId);

        if (importedDatasets.size() == 0) {
            throw new DatasetNotFoundException(String.format("No datasets found in import data that is part of catalog %s", catalogId ));
        }

        catalogToImportTo.setDataset(importedDatasets);

        return catalogToImportTo;
    }

    Catalog parseCatalog(Model model, Catalog existingCatalog, String catalogId) throws  IOException, CatalogNotFoundException {
        List<Catalog> catalogs = parseCatalogs(model);

        Catalog catalogToImportTo = catalogs
                .stream()
                .filter(cat -> cat.getUri().contains(catalogId))
                .peek(cat -> logger.debug("Found catalog {} in external data", cat.toString()))
                .findFirst()
                .orElseThrow(() -> new CatalogNotFoundException(String.format("Catalog %s is not found in import data", catalogId)));

        // Ignore imported catalog attributes, i.e. copy over stored values to result
        catalogToImportTo.setId(existingCatalog.getId());
        catalogToImportTo.setTitle(existingCatalog.getTitle());
        catalogToImportTo.setDescription(existingCatalog.getDescription());
        catalogToImportTo.setPublisher(existingCatalog.getPublisher());

        return catalogToImportTo;
    }

    List<Dataset> parseAndSaveDatasets(Model model, Catalog catalogToImportTo, String catalogId) throws IOException {
        List<Dataset> importedDatasets = new ArrayList<>();

        List<Dataset> datasets = parseDatasets(model);

        for (Dataset dataset : datasets) {
            if (dataset.getCatalog() != null && dataset.getCatalog().contains(catalogId)) {

                Dataset newDataset = datasetController.createAndSaveDataset(catalogId, dataset, catalogToImportTo);
                importedDatasets.add(newDataset);
                logger.trace("ds: {}", newDataset);
            }
        }

        return importedDatasets;
    }

    List<Catalog> parseCatalogs(Model model) throws IOException {

        String json = frame(DatasetFactory.create(model), IOUtils.toString(new ClassPathResource("frames/catalog.json").getInputStream(), "UTF-8"));

        return new Gson().fromJson(json, FramedCatalog.class).getGraph();
    }

    List<Dataset> parseDatasets(Model model) throws IOException {

        // using owl ontology to get inverse relations from dataset to catalog
        InfModel modelWithInverseCatalogRelations = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), owlSchema, model);

        String json = frame(DatasetFactory.create(modelWithInverseCatalogRelations),
                IOUtils.toString(new ClassPathResource("frames/dataset.json").getInputStream(), "UTF-8"));

        logger.trace("json after frame: {}",json);
        String preprocessedJson = preProcessDatasetAttributes(json);

        logger.trace("json after preprocessing: {}", preprocessedJson);

        List<Dataset> result = new Gson().fromJson(preprocessedJson, FramedDataset.class).getGraph();

        postprosessDatasetAttributes(result);

        logger.trace("Result frame transformation: {}", new GsonBuilder().setPrettyPrinting().create().toJson(result));
        logger.info("parsed {} datasets from RDF import", result.size());

        return result;
    }

    private String preProcessDatasetAttributes(String json) {
        Gson gson = new GsonBuilder().create();

        JsonObject model = gson.fromJson(json, JsonObject.class);
        JsonArray datasets = model.getAsJsonArray("@graph");

        datasets.forEach( (JsonElement dataset) -> {
            // preprocess temporals because framing cannot convert object structure
            JsonArray temporals = dataset.getAsJsonObject().getAsJsonArray("temporal");
            if (temporals != null) {
                temporals.forEach((JsonElement t) -> {
                    JsonElement hasBeginning = t.getAsJsonObject().getAsJsonObject("owt:hasBeginning").getAsJsonObject("owt:inXSDDateTime").getAsJsonPrimitive("@value");
                    JsonElement hasEnd = t.getAsJsonObject().getAsJsonObject("owt:hasEnd").getAsJsonObject("owt:inXSDDateTime").getAsJsonPrimitive("@value");
                    t.getAsJsonObject().add("startDate", hasBeginning);
                    t.getAsJsonObject().add("endDate", hasEnd);
                });
            }

            JsonObject publisher = dataset.getAsJsonObject().getAsJsonObject("publisher");
            if (publisher != null) {
                JsonElement publisherName = publisher.get("name");
                if (publisherName != null && publisherName instanceof JsonArray) {
                    logger.warn("Publisher has multiple names: {}", publisherName.toString());
                    JsonArray nameArray = (JsonArray) publisherName;
                    if (nameArray.size() >= 1) {
                        publisher.add("name", nameArray.get(0));
                    }
                }
            }

            // handle SkosConcepts: conformsTo
            handleSkosConcepts(dataset, "conformsTo");
            handleSkosConcepts(dataset, "subject");
            handleSkosConcepts(dataset, "legalBasisForRestriction");
            handleSkosConcepts(dataset, "legalBasisForProcessing");
            handleSkosConcepts(dataset, "legalBasisForAccess");
            handleSkosConcepts(dataset, "informationModel");

            // handle extension to DCAT-AP-NO 1.1 - multiple formats
            JsonArray distributions = dataset.getAsJsonObject().getAsJsonArray("distribution");
            if (distributions != null) {
                distributions.forEach(distribution -> {
                    try {
                        JsonPrimitive format = distribution.getAsJsonObject().getAsJsonPrimitive("dct:format");
                        JsonArray array = new JsonArray();
                        array.add(format);
                        distribution.getAsJsonObject().add("format", array);
                    } catch (ClassCastException cce) {
                        // do nothing. Format is either missing or is an array already
                    }


                    handleSkosConcepts(distribution, "conformsTo");
                });


            }


        });

        return gson.toJson(model);
    }

    /**
     * Converts arrays of string(uris) to SkosConcepts with uri.
     *
     * No attempt at guessing prefLabel.
     *
     * @param dataset
     * @param propertyName
     */
    void handleSkosConcepts(JsonElement dataset, String propertyName) {
        JsonArray conformsTo = dataset.getAsJsonObject().getAsJsonArray(propertyName);
        if (conformsTo != null) {
            JsonArray arry = new JsonArray();
            conformsTo.forEach(element -> {
                if (element instanceof JsonPrimitive) {

                    // assume array of uris
                    String uri = element.getAsString();
                    JsonObject newSkosConcept = new JsonObject();
                    newSkosConcept.addProperty("uri", uri);
                    arry.add(newSkosConcept);
                }
            });
            if (arry.size()>0) {
                dataset.getAsJsonObject().add(propertyName, arry);
            }
        }
    }

    void postprosessDatasetAttributes(List<Dataset> result) {
        try {
            fetchCodes();
        } catch (CodesImportException e) {
            logger.error("Fetch codes failed: {}", e.getLocalizedMessage(), e);
        }

        result.forEach(d -> {
            // Postprocess keywords
            if (d.getKeyword() != null) {
                d.getKeyword().forEach(k -> {
                    String lang = k.get("@language");
                    String value = k.get("@value");
                    k.clear();
                    k.put(lang, value);
                });
            }

            // themes
            if (d.getTheme() != null) {
                for (no.dcat.shared.DataTheme theme : d.getTheme()) {
                    DataTheme themeWithLabel = allThemes.get(theme.getUri());

                    if (themeWithLabel != null) {
                        theme.setTitle(themeWithLabel.getTitle());
                        theme.setCode(themeWithLabel.getCode());
                    }
                }
            }

            // accrualPeriodicity
            if (d.getAccrualPeriodicity() != null) {
                String code = d.getAccrualPeriodicity().getUri();
                d.getAccrualPeriodicity().setPrefLabel(getLabelForCode("frequency", code));
            }
            if (d.getLanguage() != null) {
                d.getLanguage().forEach(lang -> {
                    lang.setPrefLabel(getLabelForCode("linguisticsystem", lang.getUri()));
                });
            }
            if (d.getProvenance() != null) {
                d.getProvenance().setPrefLabel(getLabelForCode("provenancestatement", d.getProvenance().getUri()));
            }
            if (d.getAccessRights() != null) {
                d.getAccessRights().setPrefLabel(getLabelForCode("rightsstatement", d.getAccessRights().getUri()));
            }

        });
    }


    void fetchCodes() throws CodesImportException {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<List<String>> codeTypesResponse = restTemplate.exchange(THEMES_SERVICE_URL + "codes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {});

        if (codeTypesResponse.getStatusCode() != HttpStatus.OK) {
            throw new CodesImportException(String.format("Cannot access themes service for code types. Error %d",codeTypesResponse.getStatusCodeValue()));
        }

        for (String type : codeTypesResponse.getBody()) {
            logger.debug("fetching {}", type);
            ResponseEntity<List<SkosCode>> responseEntity = restTemplate.exchange(THEMES_SERVICE_URL + "codes/" + type,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<SkosCode>>() {});
            logger.debug("found {} SkosCode", responseEntity.getBody());

            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                throw new CodesImportException(String.format("Cannot access themes service for code type %s. Error %d",type, responseEntity.getStatusCodeValue()));
            }

            List<SkosCode> codelist = responseEntity.getBody();
            pruneLanguages(codelist);

            logger.debug("pruned codes {}",codelist);

            Map<String, SkosCode> codeMap = new HashMap<>();
            codelist.forEach(skosCode -> {
                codeMap.put(skosCode.getUri(), skosCode);
            });

            allCodes.put(type,codeMap);
        }

        // fetch themes
        ResponseEntity<List<DataTheme>> themesResponseEntity = restTemplate.exchange(THEMES_SERVICE_URL + "/themes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<DataTheme>>() {});

        if (themesResponseEntity.getStatusCode() != HttpStatus.OK) {
            throw new CodesImportException(String.format("Cannot access themes service for code types. Error %d", codeTypesResponse.getStatusCodeValue()));
        }

        for (DataTheme theme: themesResponseEntity.getBody()) {
            allThemes.put(theme.getId(), theme);
        }

    }

    private final Set<String> languages = Sets.newHashSet("no", "nb", "nn", "en");

    void pruneLanguages(List<SkosCode> codelist) {
        codelist.forEach(skosCode -> skosCode.getPrefLabel().keySet().removeIf(lang -> !languages.contains(lang)));
    }

    Map<String,String> getLabelForCode(String codeType, String code) {
        SkosCode skosCode = allCodes.get(codeType).get(code);

        if (skosCode != null) {
            return skosCode.getPrefLabel();
        }

        return null;
    }

    private String frame(org.apache.jena.query.Dataset dataset, String frame) {

        WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
        PrefixMap pm = RiotLib.prefixMap(dataset.getDefaultModel().getGraph());

        StringWriter stringWriter = new StringWriter();

        JsonLDWriteContext ctx = new JsonLDWriteContext();

        ctx.setFrame(frame);

        w.write(stringWriter, dataset.asDatasetGraph(), pm, null, ctx);


        return stringWriter.toString();
    }


}

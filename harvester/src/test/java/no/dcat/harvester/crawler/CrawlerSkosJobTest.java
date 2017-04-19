package no.dcat.harvester.crawler;

import no.dcat.admin.store.AdminDataStore;
import no.dcat.admin.store.DcatDataStore;
import no.dcat.admin.store.domain.DcatSource;
import no.dcat.harvester.crawler.handlers.FusekiResultHandler;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Created by bjg on 25.01.2017.
 */
public class CrawlerSkosJobTest {
    @Test
    public void testCrawlerSkosJob() {
        DcatSource dcatSource = new DcatSource("http//dcat.difi.no/test", "Test", "rdf/access-right-skos.rdf", "tester", "");

        DcatDataStore dcatDataStore = Mockito.mock(DcatDataStore.class);
        AdminDataStore adminDataStore = Mockito.mock(AdminDataStore.class);

        FusekiResultHandler handler = new FusekiResultHandler(dcatDataStore, null);

        handler.process(dcatSource, ModelFactory.createDefaultModel());

        CrawlerSkosJob job = new CrawlerSkosJob(dcatSource, adminDataStore, "src/test/resources/rdf/access-right-skos.rdf", handler);
        job.run();
    }
}

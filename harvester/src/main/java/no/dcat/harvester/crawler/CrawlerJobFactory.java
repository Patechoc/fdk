package no.dcat.harvester.crawler;

import com.google.common.cache.LoadingCache;
import no.dcat.harvester.crawler.handlers.ElasticSearchResultHandler;
import no.dcat.harvester.crawler.handlers.FusekiResultHandler;
import no.dcat.harvester.settings.ApplicationSettings;
import no.dcat.harvester.settings.FusekiSettings;
import no.difi.dcat.datastore.AdminDataStore;
import no.difi.dcat.datastore.DcatDataStore;
import no.difi.dcat.datastore.Fuseki;
import no.difi.dcat.datastore.domain.DcatSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;

@Component
public class CrawlerJobFactory {
	
	@Autowired
	private FusekiSettings fusekiSettings;

	@Autowired
	private ApplicationSettings applicationSettings;
	
	@Autowired
	private LoadingCache<URL, String> brregCache;
	
	private AdminDataStore adminDataStore;
	private DcatDataStore dcatDataStore;
	
	private FusekiResultHandler fusekiResultHandler;
	private ElasticSearchResultHandler elasticSearchResultHandler;

	//TODO: gjør om til property
	//private String elasticSearchClusterName = "fellesdatakatalog";

	@PostConstruct
	public void initialize() {
		adminDataStore = new AdminDataStore(new Fuseki(fusekiSettings.getAdminServiceUri()));
		dcatDataStore = new DcatDataStore(new Fuseki(fusekiSettings.getDcatServiceUri()));
		fusekiResultHandler = new FusekiResultHandler(dcatDataStore, adminDataStore);
		
	}
	
	public CrawlerJob createCrawlerJob(DcatSource dcatSource) {
		elasticSearchResultHandler = new ElasticSearchResultHandler(applicationSettings.getElasticSearchHost(), applicationSettings.getElasticSearchPort(), applicationSettings.getElasticSearchCluster());
		return new CrawlerJob(dcatSource, adminDataStore, brregCache, fusekiResultHandler, elasticSearchResultHandler);
	}



}

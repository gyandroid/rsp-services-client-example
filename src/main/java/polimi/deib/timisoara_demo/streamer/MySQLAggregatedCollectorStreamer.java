package polimi.deib.timisoara_demo.streamer;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import polimi.deib.csparql_rest_api.Csparql_Remote_API;
import polimi.deib.rsp_service4csparql_client_example.Client_Server;
import polimi.deib.timisoara_demo.ontology.MC;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class MySQLAggregatedCollectorStreamer implements Runnable {

	private String csparqlServerAddress;
	private Csparql_Remote_API csparqlAPI;
	private String streamIRI;
	private String monitoringDatumBaseIRI;
	private String mySQLIRI;

	private Logger logger = LoggerFactory.getLogger(MySQLAggregatedCollectorStreamer.class.getName());
	private int sleepTime;
	private String infrastructureIRI;
	private double probFailure;

	public MySQLAggregatedCollectorStreamer(String csparqlServerAddress, String streamIRI, Resource infrastructureClass, 
			String infrastructureIRI, String mySQLIRI, int sleepTime, double probFailure) {
		super();
		this.csparqlServerAddress = csparqlServerAddress;
		csparqlAPI = new Csparql_Remote_API(csparqlServerAddress);
		this.streamIRI = streamIRI;
		this.monitoringDatumBaseIRI = streamIRI + "/" + MC.MonitoringDatum.getLocalName() + "#";
		this.mySQLIRI = mySQLIRI;
		this.sleepTime = sleepTime;
		this.infrastructureIRI = infrastructureIRI;
		this.probFailure = probFailure;
	}

	public void run() {

		Model m;


		Random randomGenerator = new Random();
		int id;
		int attemptedConnections;
		while(true){
			
			id = Client_Server.nextID();
			attemptedConnections = randomGenerator.nextInt(100);
			m = ModelFactory.createDefaultModel();
			
			//ATTEMPTED CONNECTIONS
			m.createResource(monitoringDatumBaseIRI + Client_Server.nextID())
				.addProperty(RDF.type, MC.MonitoringDatum)
				.addProperty(MC.hasMetric, MC.AttemptedConnections)
				.addProperty(MC.hasValue,  m.createTypedLiteral(attemptedConnections, XSDDatatype.XSDinteger))
				.addProperty(MC.isAbout, m.createResource(mySQLIRI)
						.addProperty(RDF.type, MC.Platform) // .addProperty(RDF.type, MC.MySQL)
						.addProperty(MC.runsOn, m.createResource(infrastructureIRI)
								.addProperty(RDF.type, MC.Infrastructure))); // infrastructureClass
			
//			System.out.println(attemptedConnections);
//			System.out.println(Math.round((double)attemptedConnections*probFailure));
			//ABORTED CONNECTIONS
			m.createResource(monitoringDatumBaseIRI + Client_Server.nextID())
			.addProperty(RDF.type, MC.MonitoringDatum)
			.addProperty(MC.hasMetric, MC.AbortedConnections)
			.addProperty(MC.hasValue,  m.createTypedLiteral(Math.round((double)attemptedConnections*probFailure), XSDDatatype.XSDinteger))
			.addProperty(MC.isAbout, m.createResource(mySQLIRI)
					.addProperty(RDF.type, MC.Platform) // .addProperty(RDF.type, MC.MySQL)
					.addProperty(MC.runsOn, m.createResource(infrastructureIRI)
							.addProperty(RDF.type, MC.Infrastructure))); // infrastructureClass
			

			csparqlAPI.feedStream(streamIRI, m);
			
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error("Error while launching the sleep operation", e);
			}

		}
	}
}
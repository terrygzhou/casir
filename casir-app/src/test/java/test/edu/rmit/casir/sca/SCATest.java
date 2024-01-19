package test.edu.rmit.casir.sca;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.apache.tuscany.sca.assembly.Component;
import org.apache.tuscany.sca.assembly.Composite;
import org.apache.tuscany.sca.assembly.Implementation;
import org.apache.tuscany.sca.assembly.Reference;
import org.apache.tuscany.sca.assembly.Service;
import org.apache.tuscany.sca.contribution.processor.StAXArtifactProcessor;
import org.apache.tuscany.sca.contribution.processor.StAXArtifactProcessorExtensionPoint;
import org.apache.tuscany.sca.contribution.resolver.ModelResolver;
import org.apache.tuscany.sca.core.DefaultExtensionPointRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

public class SCATest {

	private static XMLInputFactory inputFactory;
	private static StAXArtifactProcessorExtensionPoint staxProcessors;
	private static ModelResolver resolver;
	String resourcePath = "sca1.x.composite"; // "rmit/sca/simple.composite";
	private static Logger logger=Logger.getLogger(test.edu.rmit.casir.sca.SCATest.class);

	@BeforeClass
	public static void setUp() throws Exception {
		DefaultExtensionPointRegistry extensionPoints = new DefaultExtensionPointRegistry();
		inputFactory = XMLInputFactory.newInstance();
		staxProcessors = extensionPoints
				.getExtensionPoint(StAXArtifactProcessorExtensionPoint.class);
//		resolver = new DefaultModelResolver();
	}

	@Test
	public void testResolveCompositeByTerry() throws Exception {
		// resourcePath file must be put in the classpath
		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
		// resourcePath is put in the same path of this class.
		// InputStream is = getClass().getResourceAsStream(resourcePath);
		StAXArtifactProcessor<Composite> compositeReader = staxProcessors.getProcessor(Composite.class);
		XMLStreamReader reader = inputFactory.createXMLStreamReader(is);
		Composite rootComposite = compositeReader.read(reader);
		is.close();
//		assertNotNull(rootComposite);
		// resolver.addModel(nestedComposite);
		// is = getClass().getResourceAsStream("TestAllCalculator.composite");
		// reader = inputFactory.createXMLStreamReader(is);
		// Composite composite = compositeReader.read(reader);
		// is.close();
		logger.info("\tRoot Composite is "+rootComposite.getName().getLocalPart());
		for (Service s : rootComposite.getServices()) {
			logger.info("\t\tservice name " + s.getName());
		}
		
		for (Reference ref : rootComposite.getReferences()) {
			logger.info("\t\treference name " + ref.getName());
		}
		
		for (Component c : rootComposite.getComponents()) {
			logger.info("\t\tcomponent name " + c.getName());
			Implementation impl = c.getImplementation();
			if (impl != null) {
				String implStr=impl.toString();
				String xyz=implStr.substring(implStr.indexOf("}")+1, implStr.length());
				logger.info("\t\tImplementation is "+xyz);
				String subCompositeFile=xyz+".composite";
				this.resourcePath=subCompositeFile;
				this.testResolveCompositeByTerry();
			}
		}
	}
}

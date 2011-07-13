package eu.interedition.text.repository;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.*;
import eu.interedition.text.mem.SimpleQName;
import eu.interedition.text.xml.SimpleXMLParserConfiguration;
import eu.interedition.text.xml.XMLParser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Transactional
public class TokenizerTest extends AbstractTest {
  private static final Logger LOG = LoggerFactory.getLogger(TokenizerTest.class);

  private static final String TEI_NS = "http://www.tei-c.org/ns/1.0";

  @Autowired
  private XMLParser parser;

  @Autowired
  private TextRepository textRepository;

  @Autowired
  private AnnotationRepository annotationRepository;

  @Autowired
  private Tokenizer tokenizer;

  @Test
  public void tokenize() throws IOException, TransformerException, XMLStreamException {
    final SimpleXMLParserConfiguration parserConfiguration = new SimpleXMLParserConfiguration();
    parserConfiguration.exclude(new SimpleQName(TEI_NS, "teiHeader"));
    parserConfiguration.addContainerElement(new SimpleQName(TEI_NS, "subst"));
    parserConfiguration.addContainerElement(new SimpleQName(TEI_NS, "choice"));
    parserConfiguration.addLineElement(new SimpleQName(TEI_NS, "lb"));
    parserConfiguration.addLineElement(new SimpleQName(TEI_NS, "pb"));

    for (String resource : new String[]{"/igntp/0101.xml", "/igntp/0105.xml", "/igntp/0109.xml"}) {
      LOG.info(Strings.padStart("Tokenizing " + resource, 100, '='));

      final Text source = parser.load(new StreamSource(getClass().getResourceAsStream(resource)));
      final Text text = parser.parse(source, parserConfiguration);

      tokenizer.tokenize(text, new WhitespaceTokenizerSettings(true));
      printTokenizedWitness(text);
    }
  }

  private void printTokenizedWitness(Text text) throws IOException {
    System.out.println(Strings.padStart(text.toString(), 80, '='));

    int read = 0;

    final SortedMap<Range, Boolean> ranges = Maps.newTreeMap();
    for (Annotation token : annotationRepository.find(text, Tokenizer.TOKEN_NAME)) {
      final Range range = token.getRange();
      if (read < range.getStart()) {
        ranges.put(new Range(read, range.getStart()), false);
      }
      ranges.put(token.getRange(), true);
      read = token.getRange().getEnd();
    }

    final int length = textRepository.length(text);
    if (read < length) {
      ranges.put(new Range(read, length), false);
    }

    final SortedMap<Range, String> texts = textRepository.bulkRead(text, Sets.newTreeSet(ranges.keySet()));
    for (Map.Entry<Range, Boolean> range : ranges.entrySet()) {
      System.out.print(range.getValue() ? "[" : "");
      System.out.print(texts.get(range.getKey()));
      System.out.print(range.getValue() ? "]" : "");
    }
    System.out.println();

  }
}
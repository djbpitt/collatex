package eu.interedition.collatex.xmltokenizer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import eu.interedition.collatex.simple.SimplePatternTokenizer;
import eu.interedition.collatex.subst.LayerDefinition;

public class VersionExtractor {

    public List<String> extractTextVersions(String xml) {
        LayerDefinition layerDefinition = LayerDefinition.getDefault();
        LayeredTextTokenizer t = new LayeredTextTokenizer(xml, SimplePatternTokenizer.BY_WS_OR_PUNCT_WITH_STARTING_WHITESPACE_ALLOWED);
        List<LayeredTextToken> layeredTextTokens = t.getLayeredTextTokens();
        List<StringBuilder> sbList = Lists.newArrayList(new StringBuilder(), new StringBuilder());
        layeredTextTokens.forEach(token -> {
            Set<String> ancestorNames = token.getAncestors().stream()//
                    .map(XMLStartElementNode::getName)//
                    .collect(toSet());
            boolean isAdd = ancestorNames.contains("add");
            boolean isDel = ancestorNames.contains("del");
            if (!isAdd && !isDel) {
                sbList.get(0).append(token.getTokenText());
                sbList.get(1).append(token.getTokenText());

            } else if (isDel) {
                sbList.get(0).append(token.getTokenText());

            } else if (isAdd) {
                sbList.get(1).append(token.getTokenText());
            }

        });

        return sbList.stream().map(StringBuilder::toString).distinct().collect(toList());
    }


}
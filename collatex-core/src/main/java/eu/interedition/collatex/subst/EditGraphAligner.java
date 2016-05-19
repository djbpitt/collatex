package eu.interedition.collatex.subst;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by ronalddekker on 30/04/16.
 * This is a special version of the edit graph aligner that can handle witnesses with substitutions in them.
 */
public class EditGraphAligner {

    final List<EditGraphTableLabel> labelsWitnessB;
    final List<EditGraphTableLabel> labelsWitnessA;
    final Score[][] cells;

    public static List<EditGraphTableLabel> createLabels(WitnessTree.WitnessNode wit_a) {
        Stream<WitnessTree.WitnessNodeEvent> nodeEventStream = wit_a.depthFirstNodeEventStream();
        // I want to group all the open , text and close events together in a group
        // I first try to do that with a reduce operation
        // but for that to work (left and right) have to be of the same type
        // we might be able to accomplish the same thing with a collect operator
        // Otherwise we have to fall back to the StreamEx extension package.
        // BiPredicate<WitnessTree.WitnessNodeEvent, WitnessTree.WitnessNodeEvent> predicate = (event1, event2) -> event1.getClass().equals(event2.getClass());
        // Two rules:
        // 1. When two text tokens follow each other we should not group them together
        // 1b. A text token followed by anything other than a close tag should not be grouped together
        // 2. When a close tag is followed by an open tag we should not group them together
        // 2b. When a close tag is followed by anything other than a close tag we should not group them together
        BiPredicate<WitnessTree.WitnessNodeEvent, WitnessTree.WitnessNodeEvent> predicate = (event1, event2) -> !(event1.type.equals(WitnessTree.WitnessNodeEventType.TEXT) && !event2.type.equals(WitnessTree.WitnessNodeEventType.END)) && !(event1.type.equals(WitnessTree.WitnessNodeEventType.END) && !event2.type.equals(WitnessTree.WitnessNodeEventType.END));

        List<List<WitnessTree.WitnessNodeEvent>> lists = nodeEventStream.collect(new GroupOnPredicateCollector<>(predicate));
        Stream<EditGraphTableLabel> editGraphTableLabelStream = lists.stream().map(list -> list.stream().collect(new LabelCollector()));
        return editGraphTableLabelStream.collect(Collectors.toList());
    }




    public EditGraphAligner(WitnessTree.WitnessNode a, WitnessTree.WitnessNode b) {
        // from the witness node trees we calculate the labels
        labelsWitnessA = createLabels(a);
        labelsWitnessB = createLabels(b);

        // from the labels we map the nodes to cell coordinates
        Map<WitnessTree.WitnessNode, Integer> nodeToXCoordinate = mapNodesToIndex(labelsWitnessA);
        Map<WitnessTree.WitnessNode, Integer> nodeToYCoordinate = mapNodesToIndex(labelsWitnessB);

        // init cells and scorer
        cells = new Score[labelsWitnessB.size()+1][labelsWitnessA.size()+1];
        Scorer scorer = new Scorer();

        // init 0,0
        cells[0][0] = new Score(0);

        // fill the first row with gaps
        IntStream.range(1, labelsWitnessA.size()+1).forEach(x -> {
            int previousX = getPreviousCoordinateForLabel(nodeToXCoordinate, labelsWitnessA.get(x-1),x-1);
            cells[0][x] = scorer.gap(cells[0][previousX]);
        });

        // fill the first column with gaps
        IntStream.range(1, labelsWitnessB.size()+1).forEach(y -> {
            int previousY = getPreviousCoordinateForLabel(nodeToYCoordinate, labelsWitnessB.get(y-1),y-1);
            cells[y][0] = scorer.gap(cells[previousY][0]);
        });

        // fill the rest of the cells in an y by x fashion
        IntStream.range(1, labelsWitnessB.size()+1).forEach(y -> {
            IntStream.range(1, labelsWitnessA.size()+1).forEach(x -> {
                EditGraphTableLabel tokenB = labelsWitnessB.get(y - 1);
                EditGraphTableLabel tokenA = labelsWitnessA.get(x - 1);
                // the previous position does not have to be y-1 or x-1
                // in the case of an OR operation.. at the start of each OR operand we have to reset the counter
                // to the value before the start of the or operator.
                // most of this could be calculated before hand and does not have to calculated again and again during the scoring

                // check whether a label (a or b) has an opening add or del
                // if so previous y and x are taken from the coordinates of the opening subst -1 (y and x)
                int previousX = getPreviousCoordinateForLabel(nodeToXCoordinate, tokenA, x-1);
                int previousY = getPreviousCoordinateForLabel(nodeToYCoordinate, tokenB, y-1);

                Score upperLeft = scorer.score(cells[previousY][previousX], tokenB, tokenA);
                Score left = scorer.gap(cells[y][previousX]);
                Score upper = scorer.gap(cells[previousY][x]);
                Score max = Collections.max(Arrays.asList(upperLeft, left, upper), (score, other) -> score.globalScore - other.globalScore);
                cells[y][x] = max;

//                // check whether a label (a or b) has a closing subst
//                // note that there can be multiple subst that end here..
//                // it will be interesting to see how we handle that
//                if (tokenB.containsEndSubst()) {
//                    // here we go look for the subst again (this can be done more efficient)
//                    WitnessTree.WitnessNode endSubstNodes = tokenB.getEndSubstNodes();
//                    // Nu hebben we een end subst node te pakken
//                    // nu moet ik alle bij behorende adds en dels zien te vinden..
//                    // dat zijn de kinderen van de betreffende subst
//                    Stream<WitnessTree.WitnessNode> childNodes = endSubstNodes.depthFirstNodeStream();
//                    // ik moet eigenlijk filteren maar dat ga ik nu even niet doen
//                    // van alle child nodes moet ik daar dan weer de laatste child van pakken
//                    childNodes.map(node -> node.depthFirstNodeStream().re
//
//                }
//
//                // if tokenB as well as tokenA contain subst tags we have to do something more complicated
//                // for now we detected that situation and exit
//                if (tokenA.containsEndSubst()) {
//                    throw new UnsupportedOperationException();
//                }

            });
        });
    }

    private int getPreviousCoordinateForLabel(Map<WitnessTree.WitnessNode, Integer> nodeToCoordinate, EditGraphTableLabel token, int defaultCoordinate) {
        if (token.containsStartAddOrDel()) { // start of an option (add / del)
            // every edit graph table label is associated with witness node (as the text)
            // we need to walk to the parent
            WitnessTree.WitnessNode currentNode = token.text;
            Stream<WitnessTree.WitnessNode> parentNodeStream = currentNode.parentNodeStream();
            Optional<WitnessTree.WitnessNode> substOptional = parentNodeStream.filter(node -> node.data.equals("subst")).findFirst();
            if (!substOptional.isPresent()) {
                throw new RuntimeException("We found an OR operand but could not find the OR operator!");
            }
            WitnessTree.WitnessNode substNode = substOptional.get();
            // convert the substNode into index in the edit graph table
            Integer index = nodeToCoordinate.get(substNode);
            defaultCoordinate = index - 1;
            // debug
            // System.out.println("Label text on the horizontal axis >"+tokenA.text+"< maps to index "+previousX);
        }
        return defaultCoordinate;
    }

    private Map<WitnessTree.WitnessNode, Integer> mapNodesToIndex(List<EditGraphTableLabel> labels) {
        // the following code has side effects. That is because Java 8 does not have an enumerate function for streams.
        Map<WitnessTree.WitnessNode, Integer> nodesToCoordinate = new HashMap<>();

        // calculate the y and x coordinates of the witness nodes in the table (so we can map from one to the other)
        IntStream.range(0, labels.size()).forEach(index -> {
            // map index to label
            EditGraphTableLabel label = labels.get(index);
            // we take the nodes that are opened at this label and map the nodes to the index
            label.startElements.forEach(node -> nodesToCoordinate.put(node, index+1));
            // we take the text node at this label and map it to the index
            nodesToCoordinate.put(label.text, index+1);
        });

        return nodesToCoordinate;
    }

    public void align() {

    }


    private static class LabelCollector implements java.util.stream.Collector<WitnessTree.WitnessNodeEvent, EditGraphTableLabel, EditGraphTableLabel> {
        @Override
        public Supplier<EditGraphTableLabel> supplier() {
            return EditGraphTableLabel::new;
        }

        @Override
        public BiConsumer<EditGraphTableLabel, WitnessTree.WitnessNodeEvent> accumulator() {
            return (label, event) ->  {
                switch (event.type) {
                    case START:
                        label.addStartEvent(event.node);
                        break;
                    case END:
                        label.addEndEvent(event.node);
                        break;
                    case TEXT:
                        label.addTextEvent(event.node);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown event type");
                }
            };
        }

        @Override
        public BinaryOperator<EditGraphTableLabel> combiner() {
            return (item, item2) -> {
                throw new UnsupportedOperationException();
            };
        }

        @Override
        public Function<EditGraphTableLabel, EditGraphTableLabel> finisher() {
            return (label) -> label;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }

    static class EditGraphTableLabel {
        List<WitnessTree.WitnessNode> startElements = new ArrayList<>();
        private List<WitnessTree.WitnessNode> endElements = new ArrayList<>();
        WitnessTree.WitnessNode text;

        public void addStartEvent(WitnessTree.WitnessNode node) {
            startElements.add(node);
        }

        public void addEndEvent(WitnessTree.WitnessNode node) {
            endElements.add(node);
        }

        public void addTextEvent(WitnessTree.WitnessNode node) {
            if (text != null) {
                throw new UnsupportedOperationException();
            }
            this.text = node;
        }

        @Override
        public String toString() {
            String a = startElements.stream().map(WitnessTree.WitnessNode::toString).collect(Collectors.joining(", "));
            String b = text.toString();
            String c = endElements.stream().map(WitnessTree.WitnessNode::toString).collect(Collectors.joining(", "));
            return b+":"+a+":"+c;
        }

        public boolean containsStartAddOrDel() {
            // find the first add or del tag...
            // Note that this implementation is from the left to right
            Optional<WitnessTree.WitnessNode> witnessNode = startElements.stream().filter(node -> node.data.equals("add") || node.data.equals("del")).findFirst();
            return witnessNode.isPresent();
        }

        public boolean containsEndSubst() {
            // find the first end subst tag...
            // if we want to do this completely correct it should be from right to left
            Optional<WitnessTree.WitnessNode> witnessNode = endElements.stream().filter(node -> node.data.equals("subst")).findFirst();
            return witnessNode.isPresent();
        }

        public WitnessTree.WitnessNode getEndSubstNodes() {
            Optional<WitnessTree.WitnessNode> witnessNode = endElements.stream().filter(node -> node.data.equals("subst")).findFirst();
            if (!witnessNode.isPresent()) {
                throw new RuntimeException("We expected one or more subst tags here!");
            }
            return witnessNode.get();
        }
    }

    public class Score {
        //TODO: set parent
        int globalScore = 0;

        public Score(int i) {
            this.globalScore = i;
        }

        public Score(Score parent) {
            this.globalScore = parent.globalScore;
        }
    }

    private class Scorer {
        public Score gap(Score parent) {
            return new Score(parent.globalScore -1);
        }

        public Score score(Score parent, EditGraphTableLabel tokenB, EditGraphTableLabel tokenA) {
            if (tokenB.text.data.equals(tokenA.text.data)) {
                return new Score(parent);
            } else {
                return new Score(parent.globalScore - 2);
            }
        }
    }
}

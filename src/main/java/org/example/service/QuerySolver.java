package org.example.service;

import org.example.domain.DTO.NodeDTO;
import org.example.domain.DTO.RequestDTO;
import org.example.domain.types.ConnectionType;
import org.example.domain.types.NodeType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuerySolver {
    private final Set<NodeDTO> activatedFeatures = new HashSet<>();
    private final Set<NodeDTO> inactivateFeatures = new HashSet<>();
    private final Set<NodeDTO> activatedHypotheses = new HashSet<>();
    private final Set<NodeDTO> solutions = new HashSet<>();
    private final Set<NodeDTO> possibleArguments = new HashSet<>();

    public List<NodeDTO> solve(RequestDTO request, List<NodeDTO> nodes) {
        List<NodeDTO> allFeatures = nodes.stream().filter(node -> node.getNodeType() == NodeType.FEATURE).toList();
        activateFeatures(allFeatures, request.getParameters()); // Шаг 0 - активация узлов-признаков

        int activatedFeaturesSize;
        int activatedHypothesesSize;

        do {
            activatedFeaturesSize = activatedFeatures.size();
            activatedHypothesesSize = activatedHypotheses.size();

            generateHypothesesSet(); // Шаг 1
            expandArgumentsSet(); // Шаг 2
            testArguments(); // Шаг 3
        } while (activatedHypothesesSize != activatedHypotheses.size() || activatedFeaturesSize != activatedFeatures.size());

        reduceHypothesesSetByRejectingArguments(); // Шаг 4

        reduceHypothesesSetByMissingArguments(); // Шаг 5

        solutions.addAll(activatedHypotheses); // Шаг 6

        if (request.isHypothesesSetDifferentiationNeeded() && activatedHypotheses.size() > 1) { // Шаг 7
            solutions.removeAll(differentiateHypothesesSet());
        }

        if (request.isHypothesesSetMinimizationNeeded() && activatedHypotheses.size() > 2) { // Шаг 8
            solutions.removeAll(minimizeExplanatorySet());
        }

        return solutions.stream().toList();
    }

    private void activateFeatures(List<NodeDTO> features, List<RequestDTO.Parameter> parameters) {
        Set<Long> featureIds = features.stream()
                .map(NodeDTO::getId)
                .collect(Collectors.toSet());

        Map<RequestDTO.Parameter, NodeDTO> parametersNodes = parameters.stream()
                .filter(parameter -> featureIds.contains(parameter.getFeatureId()))
                .collect(Collectors.toMap(
                        param -> param,
                        param -> features.stream()
                                .filter(node -> Objects.equals(node.getId(), param.getFeatureId()))
                                .findAny().orElseThrow()
                ));

        activatedFeatures.addAll(parametersNodes.entrySet().stream()
                .filter(entry -> checkFeatureActivationCondition(entry.getValue().getAttribute().getActivationCondition(),
                        entry.getKey().getAttributeValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet()));
        inactivateFeatures.addAll(parametersNodes.entrySet().stream()
                .filter(entry -> !checkFeatureActivationCondition(entry.getValue().getAttribute().getActivationCondition(),
                        entry.getKey().getAttributeValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet()));
    }

    public boolean checkFeatureActivationCondition(String activationCondion, String input) {
        if (activationCondion.matches("\\d+:\\d+")) {
            return checkRangeCondition(activationCondion, input);
        } else if (activationCondion.matches("\\[\\d+(,\\d+)*\\]")) {
            return checkDiscreteCondition(activationCondion, input);
        } else {
            return checkStringCondition(activationCondion, input);
        }
    }

    private boolean checkRangeCondition(String condition, String input) {
        String[] parts = condition.split(":");
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);

        try {
            int value = Integer.parseInt(input);
            return value >= start && value <= end;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkDiscreteCondition(String condition, String input) {
        String trimmedCondition = condition.substring(1, condition.length() - 1);
        List<String> discreteValues = Arrays.asList(trimmedCondition.split(","));
        return discreteValues.contains(input);
    }

    private boolean checkStringCondition(String condition, String input) {
        return condition.equals(input);
    }

    private void generateHypothesesSet() {
        for (NodeDTO node : activatedFeatures) {
            Map<NodeDTO, ConnectionType> connections = node.getOutgoingConnections();

            for (Map.Entry<NodeDTO, ConnectionType> entry : connections.entrySet()) {
                NodeDTO relatedNode = entry.getKey();
                ConnectionType connectionType = entry.getValue();

                if (relatedNode.getNodeType() != NodeType.FEATURE) {
                    relatedNode.getArguments().add(node);

                    if (connectionType == ConnectionType.RS) {
                        activatedHypotheses.addAll(checkAuxiliaryNode(relatedNode));
                    } else if (connectionType == ConnectionType.TRA) {
                        solutions.addAll(checkAuxiliaryNode(relatedNode));
                    }
                }
            }
        }

        activatedHypotheses.addAll(solutions);
    }

    private Set<NodeDTO> checkAuxiliaryNode(NodeDTO node) {
        if (isAndNodeActivated(node) || isOrNodeActivated(node)) {
            Set<NodeDTO> relatedHypotheses = node.getOutgoingConnections().keySet().stream().
                    filter(hypothesis -> hypothesis.getNodeType() == NodeType.HYPOTHESIS)
                    .collect(Collectors.toSet());
            relatedHypotheses.forEach(hypothesis -> hypothesis.setArguments(node.getArguments()));
            return relatedHypotheses;
        }
        return Collections.singleton(node);
    }

    public boolean isAndNodeActivated(NodeDTO node) {
        return node.getNodeType() == NodeType.AND && activatedFeatures.containsAll(node.getIncomingConnections().keySet());
    }

    public boolean isOrNodeActivated(NodeDTO node) {
        return node.getNodeType() == NodeType.OR && activatedFeatures.stream().anyMatch(node.getIncomingConnections().keySet()::contains);
    }

    private void expandArgumentsSet() {
        possibleArguments.addAll(activatedHypotheses.stream()
                .flatMap(node -> node.getOutgoingConnections().entrySet().stream()
                        .filter(entry -> entry.getKey().getNodeType() == NodeType.FEATURE
                                && entry.getValue() == ConnectionType.RS && !activatedFeatures.contains(entry.getKey()))
                        .map(Map.Entry::getKey))
                .collect(Collectors.toSet()));


    }

    private void testArguments() {
        // Запрос к клиенту для подтверждение признаков
    }

    private void reduceHypothesesSetByRejectingArguments() {
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();

        for (NodeDTO hypothesis : activatedHypotheses) {
            for (NodeDTO relatedNode : hypothesis.getIncomingConnections().keySet()) {
                if (activatedFeatures.contains(relatedNode) && hypothesis.getIncomingConnections().get(relatedNode) == ConnectionType.S) {
                    hypothesesToRemove.add(hypothesis);
                }
            }
        }
        activatedHypotheses.removeAll(hypothesesToRemove);
    }

    private void reduceHypothesesSetByMissingArguments() {
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();

        for (NodeDTO hypothesis : activatedHypotheses) {
            for (NodeDTO relatedNode : hypothesis.getOutgoingConnections().keySet()) {
                if (inactivateFeatures.contains(relatedNode) && hypothesis.getOutgoingConnections().get(relatedNode) == ConnectionType.TRA) {
                    hypothesesToRemove.add(hypothesis);
                }
            }
        }
        activatedHypotheses.removeAll(hypothesesToRemove);
    }

    private Set<NodeDTO> differentiateHypothesesSet() {
        return activatedHypotheses.stream()
                .flatMap(hypothesis -> activatedHypotheses.stream()
                        .filter(otherHypothesis -> hypothesis != otherHypothesis && hypothesis.getArguments().containsAll(otherHypothesis.getArguments()))
                        .map(otherHypothesis ->
                                hypothesis.getArguments().size() >= otherHypothesis.getArguments().size() ? otherHypothesis : hypothesis))
                .collect(Collectors.toSet());
    }

    private Set<NodeDTO> minimizeExplanatorySet() {
        boolean found = true;
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();

        while (found && activatedHypotheses.size() > 2) {
            found = false;

            for (NodeDTO hypothesis : activatedHypotheses) {
                Set<NodeDTO> combinedArguments = new HashSet<>();
                for (NodeDTO otherHypothesis : activatedHypotheses) {
                    if (!hypothesis.equals(otherHypothesis)) {
                        combinedArguments.addAll(otherHypothesis.getArguments());
                    }
                }

                if (combinedArguments.containsAll(hypothesis.getArguments())) {
                    hypothesesToRemove.add(hypothesis);
                    found = true;
                }
            }

        }
        return hypothesesToRemove;
    }
}

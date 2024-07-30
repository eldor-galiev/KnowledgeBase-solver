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
            differentiateHypothesesSet();
        }

        if (request.isHypothesesSetMinimizationNeeded() && activatedHypotheses.size() > 2) { // Шаг 8
            minimizeExplanatorySet();
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
        activatedFeatures.forEach(feature -> feature.setActivated(true));
        inactivateFeatures.addAll(parametersNodes.entrySet().stream()
                .filter(entry -> !checkFeatureActivationCondition(entry.getValue().getAttribute().getActivationCondition(),
                        entry.getKey().getAttributeValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet()));
    }

    public boolean checkFeatureActivationCondition(String activationCondition, String input) {
        if (activationCondition.matches("\\d+:\\d+")) {
            return checkRangeCondition(activationCondition, input);
        } else if (activationCondition.matches("\\[\\d+(,\\d+)*\\]")) {
            return checkDiscreteCondition(activationCondition, input);
        } else {
            return checkStringCondition(activationCondition, input);
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
        for (NodeDTO feature : activatedFeatures) {
            Map<NodeDTO, ConnectionType> connections = feature.getOutgoingConnections();

            for (Map.Entry<NodeDTO, ConnectionType> entry : connections.entrySet()) {
                NodeDTO relatedNode = entry.getKey();
                ConnectionType connectionType = entry.getValue();

                if (relatedNode.getNodeType() != NodeType.FEATURE && connectionType != ConnectionType.S) {
                    relatedNode.getArguments().add(feature);
                    activateNode(relatedNode, connectionType);
                }
            }
        }

        activatedHypotheses.addAll(solutions);
    }

    private void activateNode(NodeDTO node, ConnectionType connectionType) {
        if ((node.getNodeType() == NodeType.OR || node.getNodeType() == NodeType.AND) && isAuxiliaryNodeActivated(node)) {
            Map<NodeDTO, ConnectionType> connections = node.getOutgoingConnections().entrySet().stream()
                    .filter(entry -> entry.getKey().getNodeType() != NodeType.FEATURE
                            && entry.getValue() != ConnectionType.S)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            for (Map.Entry<NodeDTO, ConnectionType> entry : connections.entrySet()) {
                NodeDTO relatedNode = entry.getKey();
                ConnectionType relatedConnectionType = entry.getValue();

                relatedNode.getArguments().addAll(node.getArguments());
                activateNode(relatedNode, relatedConnectionType);
            }

        }
        else if (node.getNodeType() == NodeType.HYPOTHESIS) {
            if (connectionType == ConnectionType.RS) {
                activatedHypotheses.add(node);
            } else if (connectionType == ConnectionType.TRA) {
                solutions.add(node);
            }
            node.setActivated(true);
        }

    }

    public boolean isAuxiliaryNodeActivated(NodeDTO node) {
        if (node.getNodeType() == NodeType.AND
                && node.getIncomingConnections().keySet().stream().allMatch(NodeDTO::isActivated)) {
            node.setActivated(true);
            return true;
        }
        if (node.getNodeType() == NodeType.OR
                && node.getIncomingConnections().keySet().stream().anyMatch(NodeDTO::isActivated)) {
            node.setActivated(true);
            return true;
        }
        return false;
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
                    hypothesis.setActivated(false);
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
                    hypothesis.setActivated(false);
                }
            }
        }
        activatedHypotheses.removeAll(hypothesesToRemove);
    }

    private void differentiateHypothesesSet() {
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();
        for (NodeDTO hypothesis : solutions) {
            for (NodeDTO otherHypothesis : solutions) {
                if (hypothesis != otherHypothesis && hypothesis.getArguments().containsAll(otherHypothesis.getArguments())) {
                    if (hypothesis.getArguments().size() > otherHypothesis.getArguments().size()) {
                        hypothesesToRemove.add(otherHypothesis);
                        otherHypothesis.setActivated(false);
                    } else if (hypothesis.getArguments().size() < otherHypothesis.getArguments().size()) {
                        hypothesesToRemove.add(hypothesis);
                        hypothesis.setActivated(false);
                    }
                }
            }
        }
        solutions.removeAll(hypothesesToRemove);
    }

    private void minimizeExplanatorySet() {
        boolean found = true;
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();

        while (found) {
            found = false;
            solutions.removeAll(hypothesesToRemove);
            hypothesesToRemove.clear();

            for (NodeDTO hypothesis : solutions) {
                Set<NodeDTO> combinedArguments = new HashSet<>();
                for (NodeDTO otherHypothesis : solutions) {
                    if (!hypothesis.equals(otherHypothesis)) {
                        combinedArguments.addAll(otherHypothesis.getArguments());
                    }
                }

                if (combinedArguments.containsAll(hypothesis.getArguments())) {
                    hypothesesToRemove.add(hypothesis);
                    hypothesis.setActivated(false);
                    found = true;
                    break;
                }
            }

        }
    }
}

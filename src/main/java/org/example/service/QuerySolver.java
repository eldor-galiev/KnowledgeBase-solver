package org.example.service;

import org.example.domain.DTO.NodeDTO;
import org.example.domain.DTO.RequestDTO;
import org.example.domain.types.ConnectionType;
import org.example.domain.types.NodeType;

import java.util.*;
import java.util.stream.Collectors;

public class QuerySolver {

    private Set<NodeDTO> activatedFeatures;
    private Set<NodeDTO> nonactivatedFeatures;
    private Set<NodeDTO> activatedHypotheses;
    private Set<NodeDTO> solutions;
    private Set<NodeDTO> possibleArguments;

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

        if (activatedHypotheses.size() <= 1) { // Шаг 6
            solutions.addAll(activatedHypotheses);
        }

        if (request.isHypothesesSetDifferentiationNeeded() && activatedHypotheses.size() > 1) { // Шаг 7
            differentiateHypothesesSet();
        }

        if (request.isHypothesesSetMinimizationNeeded()) { // Шаг 8
            minimizeExplanatorySet();
        }

        solutions.addAll(activatedHypotheses);
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

        activatedFeatures = parametersNodes.entrySet().stream()
                .filter(entry -> entry.getValue().checkFeatureActivationCondition(entry.getKey().getAttributeValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
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
                        activatedHypotheses.add(checkAuxiliaryNode(relatedNode));
                    } else if (connectionType == ConnectionType.TRA) {
                        solutions.add(checkAuxiliaryNode(relatedNode));
                    }
                }
            }
        }

        activatedHypotheses.addAll(solutions);
    }

    private NodeDTO checkAuxiliaryNode(NodeDTO node) {
        if (isAndNodeActivated(node) || isOrNodeActivated(node)) {
            NodeDTO nextNode = node.getOutgoingConnections().keySet().stream().findFirst().orElseThrow();
            nextNode.getArguments().addAll(node.getArguments());
            return nextNode;
        }
        return node;
    }

    public boolean isAndNodeActivated(NodeDTO node) {
        return node.getNodeType() == NodeType.AND && activatedFeatures.containsAll(node.getIncomingConnections());
    }

    public boolean isOrNodeActivated(NodeDTO node) {
        return node.getNodeType() == NodeType.OR && activatedFeatures.stream().anyMatch(node.getIncomingConnections()::contains);
    }

    private void expandArgumentsSet() {
        possibleArguments = activatedHypotheses.stream()
                .flatMap(node -> node.getOutgoingConnections().entrySet().stream()
                        .filter(entry -> entry.getKey().getNodeType() == NodeType.FEATURE
                                && entry.getValue() == ConnectionType.RS)
                        .map(Map.Entry::getKey))
                .collect(Collectors.toSet());
    }

    private void testArguments() {
        // Запрос к клиенту на подтверждение признаков
    }

    private void reduceHypothesesSetByRejectingArguments() {
        Set<NodeDTO> hypothesesToRemove = new HashSet<>();

        for (NodeDTO hypothesis : activatedHypotheses) {
            for (NodeDTO relatedNode : hypothesis.getOutgoingConnections().keySet()) {
                if (activatedFeatures.contains(relatedNode) && hypothesis.getOutgoingConnections().get(relatedNode) == ConnectionType.S) {
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
                if (nonactivatedFeatures.contains(relatedNode) && hypothesis.getOutgoingConnections().get(relatedNode) == ConnectionType.TRA) {
                    hypothesesToRemove.add(hypothesis);
                }
            }
        }
        activatedHypotheses.removeAll(hypothesesToRemove);
    }

    private void differentiateHypothesesSet() {
        Set<NodeDTO> hypothesesToRemove = activatedHypotheses.stream()
                .flatMap(hypothesis -> activatedHypotheses.stream()
                        .filter(otherHypothesis -> hypothesis != otherHypothesis && hypothesis.getArguments().containsAll(otherHypothesis.getArguments()))
                        .map(otherHypothesis ->
                                hypothesis.getArguments().size() >= otherHypothesis.getArguments().size() ? otherHypothesis : hypothesis))
                .collect(Collectors.toSet());

        activatedHypotheses.removeAll(hypothesesToRemove);
    }

    private void minimizeExplanatorySet() {
        boolean found = true;

        while (found && activatedHypotheses.size() > 2) {
            found = false;

            Set<NodeDTO> hypothesesToRemove = new HashSet<>();

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

            activatedHypotheses.removeAll(hypothesesToRemove);
        }

    }
}

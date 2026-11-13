
package com.dobby.price_alert.runners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RunnerExecutionSelector {

    private static final Logger log =
            LoggerFactory.getLogger(RunnerExecutionSelector.class);

    private static final Set<String> ALLOWED_RUNNERS = Set.of(
            "fii-data",
            "index-options",
            "oi-data",
            "index-data"
    );

    private final boolean manualMode;
    private final Set<String> selectedRunners;

    public RunnerExecutionSelector(
            @Value("${app.execution-mode:automatic}") String executionMode,
            @Value("${app.runner-selection:}") String runnerSelection) {

        this.manualMode = "manual".equalsIgnoreCase(executionMode);

        if (!manualMode) {
            this.selectedRunners = Set.of();
            log.info("Automatic runner execution mode enabled.");
            return;
        }

        if (runnerSelection == null || runnerSelection.isBlank()) {
            throw new IllegalArgumentException(
                    "Manual execution requires at least one selected runner."
            );
        }

        Set<String> selected = Arrays.stream(runnerSelection.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toSet());

        Set<String> invalid = selected.stream()
                .filter(r -> !ALLOWED_RUNNERS.contains(r))
                .collect(Collectors.toSet());

        if (selected.isEmpty() || !invalid.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid runner selection: " + invalid
            );
        }

        this.selectedRunners = Set.copyOf(selected);

        log.info("Manual runner execution mode enabled.");
        log.info("Selected runners: {}", selectedRunners);
    }

    public boolean shouldRun(String runnerId) {

        // Existing scheduled workflow behaviour
        if (!manualMode) {
            return true;
        }

        boolean selected = selectedRunners.contains(runnerId);

        if (!selected) {
            log.info(
                    "Skipping {} because it was not selected.",
                    runnerId
            );
        }

        return selected;
    }
}

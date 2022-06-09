package de.metanome.algorithms.cody.codycore.runner;

import de.metanome.algorithms.cody.codycore.Configuration;
import de.metanome.algorithms.cody.codycore.Preprocessor;
import de.metanome.algorithms.cody.codycore.Validator;
import de.metanome.algorithms.cody.codycore.candidate.CheckedColumnCombination;
import de.metanome.algorithms.cody.codycore.candidate.ColumnCombination;
import de.metanome.algorithms.cody.codycore.candidate.ColumnCombinationUtils;
import de.metanome.algorithms.cody.codycore.pruning.ComponentPruner;
import de.metanome.algorithms.cody.codycore.pruning.PrunerFactory;
import com.google.common.base.Stopwatch;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ExactRunner extends BaseRunner {

    public ExactRunner(@NonNull Configuration configuration) {
        super(configuration);
    }

    /**
     * Run the approximate Cody algorithm with the set configuration
     * When finished, the results can be retrieved with getResultSet
     */
    @Override
    public void run() {
        Stopwatch completeWatch = Stopwatch.createStarted();
        log.info("Start running exact Cody algorithm with configuration: {}", this.configuration);

        Stopwatch prepareWatch = Stopwatch.createStarted();
        Preprocessor preprocessor = new Preprocessor(this.configuration);
        preprocessor.run();
        log.info("Preprocessing took: {} ms", prepareWatch.stop().elapsed(TimeUnit.MILLISECONDS));

        Stopwatch validatorWatch = Stopwatch.createStarted();
        Validator validator = new Validator(this.configuration, preprocessor.getColumnPlis(),
                preprocessor.getNRows(), preprocessor.getRowCounts());
        log.info("Unary candidate validation took: {} ms", validatorWatch.stop().elapsed(TimeUnit.MILLISECONDS));

        Stopwatch prunerWatch = Stopwatch.createStarted();
        ComponentPruner pruner = PrunerFactory.create(this.configuration, validator.getGraphView());
        pruner.run();
        log.info("Expanding unary to maximal Cody took: {} ms", prunerWatch.stop().elapsed(TimeUnit.MILLISECONDS));

        Stopwatch postProcessingWatch = Stopwatch.createStarted();
        for (ColumnCombination c : pruner.getResultSet().values())
            this.resultSet.add(validator.checkColumnCombination(c));

        this.resultSet = this.resultSet.stream().map(c -> ColumnCombinationUtils.inflateDuplicateColumns(c,
                preprocessor.getColumnIndexToDuplicatesMapping())).collect(Collectors.toList());
        log.info("Candidate post-processing took: {} ms", postProcessingWatch.stop().elapsed(TimeUnit.MILLISECONDS));

        log.info("Complete approximate Cody algorithm took: {} ms",
                completeWatch.stop().elapsed(TimeUnit.MILLISECONDS));

        log.info("ResultSet with {} Codys:", this.getResultSet().size());
        for (CheckedColumnCombination c : this.getResultSet())
            log.info("{}", c.toString(preprocessor.getColumnIndexToNameMapping()));
    }
}

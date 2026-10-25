package com.financialsurveillance.eodreconciliation.config;

import com.financialsurveillance.eodreconciliation.dto.BookingTotal;
import com.financialsurveillance.eodreconciliation.repository.ReconciliationRepository;
import com.financialsurveillance.eodreconciliation.service.ReconciliationProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

@Configuration
public class ReconciliationJobConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<BookingTotal> bookingFeedReader(
            @Value("${reconciliation.feed-dir}") String feedDir,
            @Value("#{jobParameters['tradeDate']}") String tradeDate) {
        Path file = Path.of(feedDir, "booking_totals_" + tradeDate + ".csv");
        return new FlatFileItemReaderBuilder<BookingTotal>()
                .name("bookingFeedReader")
                .resource(new FileSystemResource(file))
                .linesToSkip(1)
                .delimited()
                .names("advisorId", "tradeCount", "totalValue")
                .fieldSetMapper(fs -> new BookingTotal(
                        fs.readString("advisorId"),
                        fs.readInt("tradeCount"),
                        new BigDecimal(fs.readString("totalValue"))))
                .build();
    }

    @Bean
    @StepScope
    public ReconciliationProcessor reconciliationProcessor(
            ReconciliationRepository repository,
            @Value("#{jobParameters['tradeDate']}") String tradeDate) {
        return new ReconciliationProcessor(repository, LocalDate.parse(tradeDate));
    }

    @Bean
    @StepScope
    public ItemWriter<String> reconciledWriter(
            ReconciliationRepository repository,
            @Value("#{jobParameters['tradeDate']}") String tradeDate) {
        LocalDate date = LocalDate.parse(tradeDate);
        return chunk -> repository.markReconciled(new ArrayList<>(chunk.getItems()), date);
    }

    @Bean
    public Step reconcileStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              FlatFileItemReader<BookingTotal> bookingFeedReader,
                              ReconciliationProcessor reconciliationProcessor,
                              ItemWriter<String> reconciledWriter,
                              @Value("${reconciliation.chunk-size}") int chunkSize) {
        return new StepBuilder("reconcileStep", jobRepository)
                .<BookingTotal, String>chunk(chunkSize, transactionManager)
                .reader(bookingFeedReader)
                .processor(reconciliationProcessor)
                .writer(reconciledWriter)
                .build();
    }

    @Bean
    public Job eodReconciliationJob(JobRepository jobRepository, Step reconcileStep) {
        return new JobBuilder("eodReconciliationJob", jobRepository)
                .start(reconcileStep)
                .build();
    }
}
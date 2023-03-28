package com.example.batchprocessing;


import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfiguration {
    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;
    @Bean
    public FlatFileItemReader<Person> reader() {
        return new FlatFileItemReaderBuilder<Person>()
                .name("personItemReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited()
                .names(new String[]{"firstName", "lastName"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Person.class);
                }})
                .build();
    }

    @Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Person> jdbcWriter(DataSource dataSource) { // JPAItemWriter로도 변경할 수 있을듯?
        return new JdbcBatchItemWriterBuilder<Person>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
                .dataSource(dataSource)
                .build();
    }

    /**
     * insert 쿼리가 개별적으로 날아가 IO 비용 증가 문제가 있음.
     * 마지막에 flush()를 한번만 호출하긴 하지만...
     *
     * 영속성 기능과 spring batch간 상성이 그닥 좋지 않아보임.
     * 배치작업시엔 영속성 관리 기능을 아예 사용하지 않는게 차라리 나을 수도 있을 것으로 예상.
     */
    @Bean
    public JpaItemWriter<Person> jpaItemWriter() {
        return new JpaItemWriterBuilder<Person>()
                .entityManagerFactory(entityManagerFactory)
                // false일 경우, em.merge 메소드를 활용해 엔티티를 저장함.
                // true일 경우, em.persist 메소드를 활용해 엔티티를 저장함. -> ID가 중복될 경우, 예외를 발생시키기 때문에 ID에 대한 유일성 보장 가능.
                // writer에서 쓰는 값들이 모두 신규라면 true 옵션으로 명시해버리는 것도 괜찮을듯. dirty checking이 발생하지 않는다는걸 명시하기 위해.
                .usePersist(false)
                .build();
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository, JobCompletionNotificationListener listener, Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      JpaItemWriter<Person> writer) {
        return new StepBuilder("step1", jobRepository)
                .<Person, Person> chunk(10, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }
}

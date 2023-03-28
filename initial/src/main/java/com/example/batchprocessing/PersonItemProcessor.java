package com.example.batchprocessing;

import org.slf4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.slf4j.LoggerFactory;

public class PersonItemProcessor implements ItemProcessor<Person, Person> { // 입력과 출력 유형이 다를 수 있음.
    private static final Logger log = LoggerFactory.getLogger(PersonItemProcessor.class);

    @Override
    public Person process(Person person) throws Exception { // 인터페이스에서 throws Exception 정의.
        final String firstName = person.getFirstName();
        final String lastName = person.getLastName();

        final Person tranformedPerson = new Person(firstName, lastName);

        log.info("converting ({}) into ({})", person, tranformedPerson);

        return tranformedPerson;
    }
}

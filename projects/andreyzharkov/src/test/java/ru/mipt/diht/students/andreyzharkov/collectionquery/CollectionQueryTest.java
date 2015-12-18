package ru.mipt.diht.students.andreyzharkov.collectionquery;

import junit.framework.TestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import ru.mipt.diht.students.andreyzharkov.collectionquery.impl.EmptyCollectionException;
import ru.mipt.diht.students.andreyzharkov.collectionquery.impl.QueryExecuteException;
import ru.mipt.diht.students.andreyzharkov.collectionquery.impl.Tuple;
import ru.mipt.diht.students.andreyzharkov.collectionquery.impl.UnequalUnionClassesException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.mipt.diht.students.andreyzharkov.collectionquery.Aggregates.*;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.CollectionQueryTest.Student.student;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.Conditions.rlike;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.OrderByConditions.asc;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.OrderByConditions.desc;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.Sources.list;
import static ru.mipt.diht.students.andreyzharkov.collectionquery.impl.FromStmt.from;

/**
 * Created by Андрей on 17.12.2015.
 */
public class CollectionQueryTest extends TestCase {
    @Test
    public final void testFromExample() throws Exception {
        Iterable<Statistics> statistics =
                from(list(
                        student("ivanov", LocalDate.parse("1986-08-06"), "494"),
                        student("ivanov", LocalDate.parse("1986-08-06"), "494")))
                        .select(Statistics.class, Student::getGroup, count(Student::getGroup), avg(Student::age))
                        .where(rlike(Student::getName, ".*ov").and(s -> s.age() > 20))
                        .groupBy(Student::getGroup)
                        .having(s -> s.getCount() > 0)
                        .orderBy(asc(Student::getGroup), desc(count(Student::getGroup)))
                        .limit(100)
                        .union()
                        .from(list(student("ivanov", LocalDate.parse("1985-08-06"), "494")))
                        .selectDistinct(Statistics.class, s -> "all", count(s -> 1), avg(Student::age))
                        .execute();
        assertEquals("[Statistics{group='all', count=1, age=30}, Statistics{group='494', count=2, age=29}]",
                statistics.toString());

        Iterable<Tuple<String, String>> mentorsByStudent =
                from(list(student("ivanov", LocalDate.parse("1985-08-06"), "494")))
                        .join(list(new Group("494", "mr.sidorov")))
                        .on((s, g) -> Objects.equals(s.getGroup(), g.getGroup()))
                        .select(sg -> sg.getFirst().getName(), sg -> sg.getSecond().getMentor())
                        .execute();
        assertEquals("[Tuple{first=ivanov, second=mr.sidorov}]", mentorsByStudent.toString());

    }

    @Test
    public void testFromSelect() throws Exception {
        assertEquals(from(list(
                student("ivanov", LocalDate.parse("1985-08-06"), "491"),
                student("pejes", LocalDate.parse("1981-08-06"), "493"),
                student("kokoko", LocalDate.parse("1996-08-06"), "495"),
                student("petya", LocalDate.parse("1997-08-06"), "496"),
                student("sidorov", LocalDate.parse("1999-08-06"), "496"),
                student("petrovich", LocalDate.parse("1911-08-06"), "497"),
                student("zhdan", LocalDate.parse("1999-08-06"), "497"),
                student("pustik", LocalDate.parse("1980-08-06"), "497")))
                        .selectDistinct(String.class, Student::getGroup).stream()
                        .collect(Collectors.toList()).toString(),
                "[491, 493, 495, 496, 497]");

        assertEquals(from(list(
                student("ivanov", LocalDate.parse("1985-08-06"), "491"),
                student("pejes", LocalDate.parse("1981-08-06"), "493"),
                student("kokoko", LocalDate.parse("1996-08-06"), "495"),
                student("petya", LocalDate.parse("1997-08-06"), "496"),
                student("sidorov", LocalDate.parse("1999-08-06"), "496"),
                student("petrovich", LocalDate.parse("1911-08-06"), "497"),
                student("zhdan", LocalDate.parse("1999-08-06"), "497"),
                student("pustik", LocalDate.parse("1980-08-06"), "497")))
                        .selectDistinct(String.class, Student::getGroup)
                        .where(rlike(Student::getName, ".*").and(s -> s.age() > 0))
                        .stream()
                        .collect(Collectors.toList())
                        .toString(),
                "[491, 493, 495, 496, 497]");
    }

    @Test
    public final void testJoin() throws Exception {
        Iterable<Statistics> statistics = from(list(
                student("ivanov", LocalDate.parse("1986-08-06"), "496"),
                student("petroff", LocalDate.parse("1999-05-08"), "497"),
                student("testoff", LocalDate.parse("1987-05-08"), "497"),
                student("sidorov", LocalDate.parse("1991-08-06"), "494"),
                student("ivanov", LocalDate.parse("1988-08-06"), "493"),
                student("nobody", LocalDate.parse("1979-05-05"), "497"),
                student("testman", LocalDate.parse("1987-04-06"), "494"),
                student("someone", LocalDate.parse("1989-05-06"), "493")))
                .join(list(new Group("493", "Master"), new Group("497", "Flomaster")))
                .on(sg -> sg.getGroup(), sg -> sg.getGroup())
                .select(Statistics.class, sg -> sg.getFirst().getGroup(), count(sg -> sg.getFirst().getGroup()),
                        min(sg -> sg.getFirst().age()))
                .where(sg -> sg.getFirst().age() > 20)
                .execute();
        assertEquals("[Statistics{group='493', count=1, age=27}]", statistics.toString());
    }

    @Test
    public final void testGroupBy() throws Exception {
        Iterable<Statistics> statistics =
                from(list(
                        student("ivanov", LocalDate.parse("1985-08-06"), "491"),
                        student("pejes", LocalDate.parse("1981-08-06"), "493"),
                        student("kokoko", LocalDate.parse("1996-08-06"), "495"),
                        student("petya", LocalDate.parse("1997-08-06"), "496"),
                        student("sidorov", LocalDate.parse("1999-08-06"), "496"),
                        student("petrovich", LocalDate.parse("1911-08-06"), "497"),
                        student("zhdan", LocalDate.parse("1999-08-06"), "497"),
                        student("pustik", LocalDate.parse("1980-08-06"), "497")))
                        .select(Statistics.class, Student::getGroup, count(Student::getGroup), avg(Student::age))
                        .where(rlike(Student::getName, ".*v*").and(s -> s.age() > 25))
                        .groupBy(Student::getGroup)
                        .having(s -> s.getCount() > 0)
                        .orderBy(asc(Student::getGroup), desc(count(Student::getGroup)))
                        .limit(100)
                        .union()
                        .from(list(student("ivanov", LocalDate.parse("1985-08-06"), "494")))
                        .selectDistinct(Statistics.class, Student::getGroup, count(s -> 1), avg(Student::age))
                        .execute();
        assertEquals("[Statistics{group='494', count=1, age=30}, Statistics{group='491', count=1, age=30}, "
                        + "Statistics{group='493', count=1, age=34}, Statistics{group='497', count=2, age=69}]",
                statistics.toString());

        Iterable<Statistics> statistics2 =
                from(list(
                        student("ivanov", LocalDate.parse("1986-08-06"), "496"),
                        student("petroff", LocalDate.parse("1999-05-08"), "497"),
                        student("testoff", LocalDate.parse("1987-05-08"), "497"),
                        student("sidorov", LocalDate.parse("1991-08-06"), "494"),
                        student("ivanov", LocalDate.parse("1988-08-06"), "493"),
                        student("nobody", LocalDate.parse("1979-05-05"), "497"),
                        student("testman", LocalDate.parse("1987-04-06"), "494"),
                        student("someone", LocalDate.parse("1989-05-06"), "493")))
                        .select(Statistics.class, Student::getGroup, count(Student::getGroup), avg(Student::age))
                        .where(rlike(Student::getName, ".*v*").and(s -> s.age() > 30))
                        .groupBy(Student::getGroup)
                        .having(s -> s.getCount() > 0)
                        .orderBy(asc(Student::getGroup), desc(count(Student::getGroup)))
                        .limit(100)
                        .union()
                        .from(list(student("ivanov", LocalDate.parse("1985-08-06"), "494")))
                        .selectDistinct(Statistics.class, Student::getGroup, count(s -> 1), avg(Student::age))
                        .execute();
        assertEquals("[Statistics{group='494', count=1, age=30}, Statistics{group='497', count=1, age=36}]",
                statistics2.toString());
    }

    @Test
    public final void testUnion() throws Exception {
        Iterable<Student> students =
                from(list(
                        student("ivanov", LocalDate.parse("1986-08-06"), "496"),
                        student("petroff", LocalDate.parse("1999-05-08"), "497"),
                        student("testoff", LocalDate.parse("1987-05-08"), "497"),
                        student("sidorov", LocalDate.parse("1991-08-06"), "494"),
                        student("ivanov", LocalDate.parse("1988-08-06"), "493"),
                        student("nobody", LocalDate.parse("1979-05-05"), "497"),
                        student("testman", LocalDate.parse("1987-04-06"), "494"),
                        student("someone", LocalDate.parse("1989-05-06"), "493")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, ".*o*").and(s -> s.age() > 25))
                        .limit(3)
                        .union()
                        .from(list(student("ivanov", LocalDate.parse("1985-08-06"), "491"),
                                student("pejes", LocalDate.parse("1981-08-06"), "493"),
                                student("kokoko", LocalDate.parse("1996-08-06"), "495"),
                                student("petya", LocalDate.parse("1997-08-06"), "496"),
                                student("sidorov", LocalDate.parse("1999-08-06"), "496"),
                                student("petrovich", LocalDate.parse("1911-08-06"), "497"),
                                student("zhdan", LocalDate.parse("1999-08-06"), "497"),
                                student("pustik", LocalDate.parse("1980-08-06"), "497")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, "zhdan"))
                        .limit(5)
                        .execute();
        assertEquals("[zhdan 497, ivanov 496, testoff 497, ivanov 493]", students.toString());

        Iterable<Student> students2 =
                from(list(
                        student("petroff", LocalDate.parse("1999-05-08"), "497"),
                        student("testoff", LocalDate.parse("1987-05-08"), "497")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, ".*o*").and(s -> s.age() > 25))
                        .limit(3)
                        .union()
                        .from(list(student("pejes", LocalDate.parse("1981-08-06"), "493"),
                                student("kokoko", LocalDate.parse("1996-08-06"), "495")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, "pejes"))
                        .union()
                        .from(list(student("petrovich", LocalDate.parse("1911-08-06"), "497"),
                                student("zhdan", LocalDate.parse("1999-08-06"), "494"),
                                student("pustik", LocalDate.parse("1980-08-06"), "494")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getGroup, "494"))
                        .union()
                        .from(list(student("ibrogimovich", LocalDate.parse("1911-08-06"), "497"),
                                student("zhirkov", LocalDate.parse("1999-08-06"), "495"),
                                student("gauss", LocalDate.parse("1980-08-06"), "496")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, "no"))
                        .union()
                        .from(list(student("matan", LocalDate.parse("1911-08-06"), "491"),
                                student("algebra", LocalDate.parse("1999-08-06"), "492"),
                                student("geometry", LocalDate.parse("1980-08-06"), "493")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getName, "zhdan"))
                        .union()
                        .from(list(student("musatov", LocalDate.parse("1911-08-06"), "498"),
                                student("raigor", LocalDate.parse("1999-08-06"), "499"),
                                student("chernov", LocalDate.parse("1980-08-06"), "498")))
                        .select(Student.class, Student::getName, Student::getDateOfBith, Student::getGroup)
                        .where(rlike(Student::getGroup, "499"))
                        .execute();

        assertEquals("[raigor 499, zhdan 494, pustik 494, pejes 493, testoff 497]", students2.toString());
    }

    public static class Student {
        private final String name;

        private final LocalDate dateOfBith;

        private final String group;

        public final String getName() {
            return name;
        }

        public Student(String nme, LocalDate datOfBith, String grop) {
            this.name = nme;
            this.dateOfBith = datOfBith;
            this.group = grop;
        }

        public final LocalDate getDateOfBith() {
            return dateOfBith;
        }

        public final String getGroup() {
            return group;
        }

        public final long age() {
            return ChronoUnit.YEARS.between(getDateOfBith(), LocalDateTime.now());
        }

        public static Student student(String name, LocalDate dateOfBith, String group) {
            return new Student(name, dateOfBith, group);
        }

        public final String toString() {
            return name + " " + group;
        }
    }

    public static class Group {
        private final String group;
        private final String mentor;

        public Group(String grop, String mentr) {
            this.group = grop;
            this.mentor = mentr;
        }

        public final String getGroup() {
            return group;
        }

        public final String getMentor() {
            return mentor;
        }
    }


    public static class Statistics {

        private final String group;
        private final Long count;
        private final Long age;

        public final String getGroup() {
            return group;
        }

        public final Long getCount() {
            return count;
        }

        public final Long getAge() {
            return age;
        }

        public Statistics(String grop, Long cnt, Long ag) {
            this.group = grop;
            this.count = cnt;
            this.age = ag;
        }

        @Override
        public final String toString() {
            return "Statistics{"
                    + "group='" + group + '\''
                    + ", count=" + count
                    + ", age=" + age
                    + '}';
        }
    }
}
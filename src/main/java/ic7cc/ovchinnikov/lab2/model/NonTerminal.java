package ic7cc.ovchinnikov.lab2.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
public class NonTerminal extends Symbol {

    public NonTerminal(String name) {
        super(name, Type.NON_TERM);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonTerminal nonTerminal = (NonTerminal) o;
        return Objects.equals(getName(), nonTerminal.getName()) &&
                getType() == nonTerminal.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType());
    }

    @Override
    public String toString() {
        return "NonTerminal{" +
                "name='" + getName() + '\'' +
                '}';
    }
}

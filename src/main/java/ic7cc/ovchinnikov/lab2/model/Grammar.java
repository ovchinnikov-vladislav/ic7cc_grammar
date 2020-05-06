package ic7cc.ovchinnikov.lab2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.security.InvalidParameterException;
import java.util.*;

@ToString
@EqualsAndHashCode
public class Grammar {

    private final String name;
    @JsonIgnoreProperties( { "type" })
    private final Set<Terminal> terminals;
    @JsonIgnoreProperties( { "type" })
    private final Set<NonTerminal> nonTerminals;
    private final Set<Production> productions;
    @JsonIgnoreProperties( { "type" })
    private final NonTerminal startSymbol;

    public Grammar(String name, String startSymbol) {
        this.name = name;
        this.terminals = new LinkedHashSet<>();
        this.nonTerminals = new LinkedHashSet<>();
        this.productions = new LinkedHashSet<>();
        this.startSymbol = new NonTerminal(startSymbol);
        this.nonTerminals.add(this.startSymbol);
    }

    public boolean addTerminals(Terminal... terminals) {
        return this.terminals.addAll(Arrays.asList(terminals));
    }

    public boolean addNonTerminals(NonTerminal... nonTerminals) {
        return this.nonTerminals.addAll(Arrays.asList(nonTerminals));
    }

    public boolean addProduction(NonTerminal lhs, Symbol... rhs) {
        if (!nonTerminals.contains(lhs))
            throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal  {"+lhs+"}");
        for (Symbol symbol : rhs) {
            if (symbol instanceof Terminal) {
                if (!terminals.contains(symbol))
                    throw new InvalidParameterException("Terminals doesnt contain Terminal {"+symbol+"}");
            } else if (symbol instanceof NonTerminal) {
                if (!nonTerminals.contains(symbol))
                    throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal {"+symbol+"}");
            }
        }
        List<Symbol> resultRhs = new LinkedList<>(Arrays.asList(rhs));
        for (Symbol symbol : rhs) {
            if (symbol.getType() == Symbol.Type.EPS) {
                resultRhs.remove(symbol);
            }
        }
        if (resultRhs.size() == 0)
            resultRhs.add(Terminal.EPSILON);

        return productions.add(new Production(lhs, resultRhs));
    }

    public boolean addProduction(Production production) {
        if (!nonTerminals.contains(production.getLhs()))
            throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal  {"+production.getLhs()+"}");
        for (Symbol symbol : production.getRhs()) {
            if (symbol instanceof Terminal) {
                if (!terminals.contains(symbol))
                    throw new InvalidParameterException("Terminals doesnt contain Terminal {"+symbol+"}");
            } else if (symbol instanceof NonTerminal) {
                if (!nonTerminals.contains(symbol))
                    throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal {"+symbol+"}");
            }
        }
        return productions.add(new Production(production.getLhs(), new LinkedList<>(production.getRhs())));
    }

    public boolean removeProduction(Production production) {
        return productions.remove(production);
    }

    public String getName() {
        return name;
    }

    public Set<Terminal> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public Set<NonTerminal> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    public Set<Production> getProductions() {
        return Collections.unmodifiableSet(productions);
    }

    public NonTerminal getStartSymbol() {
        return startSymbol;
    }

    public Set<Production> findProductionsByLhs(Symbol lhs) {
        Set<Production> resultProductions = new HashSet<>();
        for (Production production : productions) {
            if (production.getLhs().equals(lhs))
                resultProductions.add(production);
        }
        return resultProductions;
    }

    public boolean isValid() {
        if (terminals.isEmpty() || nonTerminals.isEmpty() || productions.isEmpty())
            return false;
        if (startSymbol == null)
            return false;
        return nonTerminals.contains(startSymbol);
    }
}

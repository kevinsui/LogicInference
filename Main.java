import java.io.*;
import java.util.*;

public class Main {

  private static String[] clauses;
  private static String substitute;
  private static ArrayList<AtomicSentence> facts;
  private static ArrayList<Implication> implications;

  public static class AtomicSentence {
    String predicate;
    String[] arguments;
    public AtomicSentence(String predicate, String[] args) {
      this.predicate = predicate;
      this.arguments = args;
    }
  }

  public static class Implication {
    AtomicSentence[] conjuncts;
    AtomicSentence conclusion;
    public Implication(AtomicSentence[] conjuncts, AtomicSentence conclusion) {
      this.conjuncts = conjuncts;
      this.conclusion = conclusion;
    }
  }

  public static void main(String[] args) {
    try {
      BufferedReader br = new BufferedReader(new FileReader("input.txt"));
      String query = br.readLine();
      int n = Integer.parseInt(br.readLine());
      clauses = new String[n];
      for (int i = 0; i < n; i++) {
        clauses[i] = br.readLine();
      }
      // initialize arrays
      facts = new ArrayList<AtomicSentence>();
      implications = new ArrayList<Implication>();
      // add each clause to KB
      for (int i = 0; i < n; i++) {
        tellKnowledgeBase(clauses[i]);
      }
      // ask KB for answer to query
      ArrayList<AtomicSentence> goal = new ArrayList<AtomicSentence>();
      goal.add(getAtomicSentence(query));
      Boolean result = askKnowledgeBaseAND(goal);
      PrintWriter writer = new PrintWriter("output.txt", "UTF-8");
      writer.println(result.toString().toUpperCase());
      writer.close();
      System.out.println(result);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static AtomicSentence getAtomicSentence(String sentence) {
    sentence = sentence.substring(0, sentence.length()-1);
    String predicate = sentence.split("\\(")[0];                                      
    String[] args = sentence.split("\\(")[1].split(",");
    return new AtomicSentence(predicate, args);                                 
  }

  private static Boolean equalAtomicSentences(AtomicSentence as1, AtomicSentence as2) {
    if (!as1.predicate.equals(as2.predicate))
      return false;
    if (as1.arguments.length != as2.arguments.length)
      return false;
    for (int i = 0; i < as1.arguments.length; i++) {
      if (!as1.arguments[i].equals(as2.arguments[i]))
        return false;
    }
    return true;
  }

  private static String findSubstitute(AtomicSentence as1, AtomicSentence as2) {
    if (!as1.predicate.equals(as2.predicate)) {
      return null;
    }
    if (as1.arguments.length != as2.arguments.length) {
      return null;
    }
    String x = null;
    for (int i = 0; i < as1.arguments.length; i++) {
      // arguments are not equal
      if (!as1.arguments[i].equals(as2.arguments[i])) {
        // try to find a substitution
        if (as1.arguments[i].equals("x") && x == null) {
          x = as2.arguments[i];
        } else if (as2.arguments[i].equals("x") && x == null) {
          x = as1.arguments[i];
        } else {
          return null;
        }
      }
    }
    System.out.println("substitute success: x = " + x);
    return x;
  }

  private static AtomicSentence substituteVariable(AtomicSentence as, String substitute) {
    String[] newArgs = new String[as.arguments.length];
    for (int i = 0; i < newArgs.length; i++) {
      if (as.arguments[i].equals("x")) {
        newArgs[i] = substitute;
      } else {
        newArgs[i] = as.arguments[i];
      }
    }
    return (new AtomicSentence(as.predicate, newArgs));
  }

  private static void tellKnowledgeBase(String clause) {
    // check if the clause is a fact vs implication
    if (clause.contains("=>")) {
      String[] splitImplication = clause.split("=>");
      String[] splitConjuncts = splitImplication[0].split("&");
      AtomicSentence conclusion = getAtomicSentence(splitImplication[1]);
      AtomicSentence[] conjuncts = new AtomicSentence[splitConjuncts.length];
      for (int i = 0; i < splitConjuncts.length; i++) {
        conjuncts[i] = getAtomicSentence(splitConjuncts[i]);
      }
      implications.add(new Implication(conjuncts, conclusion));
    } else {
      facts.add(getAtomicSentence(clause));
    }
  }

  private static Boolean askKnowledgeBaseAND(ArrayList<AtomicSentence> goal) {
    System.out.println("ASK-AND");
    // repeat until all goal conditions are met
    while (!goal.isEmpty()) {
      AtomicSentence query = goal.remove(goal.size()-1);
      printAS("new query:", query);
      // search through facts to find a match
      Boolean matchFound = false;
      for (AtomicSentence fact : facts) {
        // compare query with facts in KB
        if (equalAtomicSentences(fact, query)) {
          printAS("match found:", query);
          matchFound = true;
          break;
        }
      }
      if (matchFound) {
        continue;
      }
      // no fact matched, substitute variables and try again
      AtomicSentence subQuery = query;
      if (substitute == null){
        // attempt to find substitute
        for (AtomicSentence fact : facts) {
          String x = findSubstitute(query, fact);
          if (x != null) {
            printAS("match found after finding x=" + x + ":", query);
            substitute = x;
            matchFound = true;
            break;
          }
        }
      } else {
        // search facts for match with substitute
        subQuery = substituteVariable(query, substitute);
        for (AtomicSentence fact : facts) {
          if (equalAtomicSentences(fact, subQuery)) {
            printAS("match found using x=" + substitute + ":", subQuery);
            matchFound = true;
            break;
          }
        }
      }
      if (matchFound) {
        continue;
      }
      if (!askKnowledgeBaseOR(query)) {
        return false;
      }
    }
    return true;
  }

  public static Boolean askKnowledgeBaseOR (AtomicSentence query) {
    System.out.println("ASK-OR");
    // no fact was matched, search through implications
    ArrayList<AtomicSentence> newGoals = new ArrayList<AtomicSentence>();
    for (Implication entry : implications) {
      // add all conjuncts to search list
      if (equalAtomicSentences(entry.conclusion, query)) {
        printAS("implication match:", entry.conclusion);
        for (AtomicSentence conjunct : entry.conjuncts) {
          printAS("adding goal:", conjunct);
          newGoals.add(conjunct);
        }
      } else if (substitute == null) {
        String x = findSubstitute(entry.conclusion, query);
        if (x != null) {
          // substitution successfully found for the variable x
          printAS("implication match:", entry.conclusion);
          substitute = x;
          for (AtomicSentence conjunct : entry.conjuncts) {
            printAS("adding goal:", conjunct);
            newGoals.add(conjunct);
          }
        }
      } else {
        String x = findSubstitute(entry.conclusion, query);
        if (x != null && x.equals(substitute)) {
          printAS("implication match:", entry.conclusion);
          for (AtomicSentence conjunct : entry.conjuncts) {
            printAS("adding goal:", conjunct);
            newGoals.add(conjunct);
          }
        }
      }
      if (!newGoals.isEmpty() && askKnowledgeBaseAND(newGoals)){
        return true;
      }
    }
    return false;
  }

  private static void printAS(String desc, AtomicSentence sentence) {
    System.out.print(desc + " " + sentence.predicate + " ");              
    for (String arg : sentence.arguments) {
      System.out.print(arg + " ");
    }
    System.out.println();
  }

}
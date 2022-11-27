import java.util.*;
import java.lang.*;
import java.util.stream.Stream;


class Potion {
    int id;
    int[] delta;
    int price;
    int bonus;

    public Potion(int id, int[] delta, int price, int bonus) {
        this.id = id;
        this.delta = delta;
        this.price = price;
        this.bonus = bonus;
    }


    int evalDist(int[] inv) {
        int[] missingIngredients = new int[4];
        for (int i = 0; i < 4; i++) {
            missingIngredients[i] = Math.min(this.delta[i] + inv[i], 0);
        }
        return Math.abs(missingIngredients[0] + missingIngredients[1] * 2 + missingIngredients[2] * 4 + missingIngredients[3] * 8);
    }

    @Override
    public String toString() {
        return "" + this.id + " " + Arrays.toString(this.delta) + " " + this.price;
    }
}


class Spell implements Cloneable {
    int id;
    int[] delta;
    boolean castable;
    boolean repeatable;

    public Spell(int id, int[] delta, boolean castable, boolean repeatable) {
        this.id = id;
        this.delta = delta;
        this.castable = castable;
        this.repeatable = repeatable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spell spell = (Spell) o;
        return id == spell.id && castable == spell.castable && repeatable == spell.repeatable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, castable, repeatable);
    }


    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "" + id + " " + Arrays.toString(delta) + " " + (castable ? 1 : 0) + "." + (repeatable ? 1 : 0) + " hasCode = " + hashCode();
    }

}

class Action {
    String type;
    int id;
    int times;
    int[] delta;

    public Action(String cast, int id, int[] delta, int times) {
        this.type = cast;
        this.id = id;
        this.delta = delta;
        this.times = times;
    }

    public Action(String rest) {
        this.type = rest;
    }

    public Action(String type, int id) {
        this.type = type;
        this.id = id;
    }

    public Action() {

    }


    @Override
    public String toString() {
        try {
            if (type.equals("CAST"))
                return "CAST" + " " + id + " " + times;
            else if (type.equals("BREW"))
                return "BREW" + " " + id;
            else if (type.equals("LEARN"))
                return "LEARN" + " " + id;
            else
                return "REST";
        } catch (Exception e) {
            System.err.println("FATAL ERROR");
            System.err.println(id);
        }
        return null;
    }
}

class Node implements Cloneable {
    int[] inv;
    ArrayList<Spell> spells;
    Action lastAction;
    Node parent;

    public Node(int[] inv, ArrayList<Spell> spells, Action lastAction) {
        this.inv = inv;
        this.spells = spells;
        this.lastAction = lastAction;
    }

    public Object clone() throws CloneNotSupportedException {
        Node clone = (Node) super.clone();
        ArrayList<Spell> newSpells = new ArrayList<>();
        for (Spell s : spells)
            newSpells.add((Spell) s.clone());
        clone.spells = newSpells; // deepcopy
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Arrays.equals(inv, node.inv) && spells.equals(node.spells);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(spells);
        result = 31 * result + Arrays.hashCode(inv);
        return result;
    }


    @Override
    public String toString() {
        Node parentNode = this;
        String message = "";
        int tabCount = 0;
        while (parentNode != null) {
            Stream<Object> castList = parentNode.spells.stream().map((spell) -> "" + spell.id + "." + (spell.castable ? 1 : 0) + "." + (spell.repeatable ? 1 : 0));
            String repeatedTab = new String(new char[++tabCount]).replace("\0", "\t");
            message += "[" + parentNode.lastAction + "] " + Arrays.toString(parentNode.inv) + " " + Arrays.toString(castList.toArray()) + " [" + parentNode.hashCode() + " ]\n" + repeatedTab;
            parentNode = parentNode.parent;
        }
        return message;
    }


}

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    // Peser les différentes couleurs
    static final double M5_EVAL_BLUE = 1.0;
    static final double M5_EVAL_GREEN = 1.2;
    static final double M5_EVAL_ORANGE = 1.4;
    static final double M5_EVAL_YELLOW = 1.6;
    static final double M1_EVAL_REPEAT_POWER = 1.7;

    static final int NB_INV_MAX = 10;

    static final int TIME_OUT = 50;
    static final int PRUNE_FACTOR = 2;
    static final int MAX_SPELLS = 14;
    static final int REST_THRESHOLD = 7;
    static int nodesCount = 0;

    static int[] somme(int[] array1, int[] array2) {
        int[] result = new int[4];
        for (int i = 0; i < 4; i++)
            result[i] = array1[i] + array2[i];
        return result;
    }

    static double norm(int[] arr) {
        return Math.sqrt(arr[0] ^ 2 + arr[1] ^ 2 + arr[2] ^ 2 + arr[3] ^ 2);
    }


    static boolean canBrew(int[] inv, Potion potion) {
        for (int i = 0; i < 4; i++)
            if (inv[i] + potion.delta[i] < 0)
                return false;
        return true;
    }

    static boolean canCast(int[] inv, Spell s, int times) {
        if (!s.castable)
            return false;
        if (times == 1) {
            int[] delta = somme(s.delta, inv);
            for (int i = 0; i < 4; i++)
                if (delta[i] < 0)
                    return false;
            return true;
        } else if (times == 2 && s.repeatable) {
            int[] delta = somme(somme(s.delta, s.delta), inv);
            for (int i = 0; i < 4; i++)
                if (delta[i] < 0)
                    return false;
            return true;
        }
        return false;
    }

    static List<Action> getCastActions(Node node) {
        ArrayList<Action> actions = new ArrayList<>();
        for (Spell s : node.spells) {
            if (canCast(node.inv, s, 2))
                actions.add(new Action("CAST", s.id, s.delta, 2));
            else if (canCast(node.inv, s, 1))
                actions.add(new Action("CAST", s.id, s.delta, 1));
        }
        return actions;
    }

    static boolean exhaustedSpells(List<Spell> spells) {
        for (Spell s : spells) {
            if (!s.castable)
                return true;
        }
        return false;
    }

    static Spell getSpell(int id, List<Spell> spells) {
        for (Spell s : spells) {
            if (s.id == id)
                return s;
        }
        return null;
    }

    static Node Bfs(Node node, Potion potion) throws CloneNotSupportedException {
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        HashSet<Node> visited = new HashSet<>(100);
        visited.add(node);
        long bfsStart = System.currentTimeMillis();
        while (!queue.isEmpty() && System.currentTimeMillis() - bfsStart < TIME_OUT) {
            // while (!queue.isEmpty()) {
            node = queue.poll();
            List<Action> actions = getCastActions(node);
//            Collections.shuffle(actions);
//            actions = actions.subList(0, PRUNE_FACTOR);
            //if (expiredSpells(node.spells) && actions.size() < REST_THRESHOLD)
            if (exhaustedSpells(node.spells))
                actions.add(new Action("REST"));
            for (Action action : actions) {
                Node childNode = (Node) node.clone();
                childNode.parent = node;
                childNode.lastAction = action;
                if (action.type.equals("CAST")) {
                    int[] newInv = somme(node.inv, action.delta);
                    if (action.times == 2)
                        newInv = somme(newInv, action.delta);
                    if (newInv[0] + newInv[1] + newInv[2] + newInv[3] > 10)
                        continue;
                    childNode.inv = newInv;
                    if (canBrew(childNode.inv, potion))
                        return childNode;
                    Spell spell = getSpell(action.id, childNode.spells);
                    spell.castable = false;
                } else if (action.type.equals("REST")) {
                    for (Spell s : childNode.spells)
                        s.castable = true;
                }
                if (visited.contains(childNode)) {
                    //System.err.println("childNode = " + childNode + " visited!");
                    continue;
                }
                queue.add(childNode);
                visited.add(childNode);
                nodesCount++;
            }
        }
        return null;
    }

    public static void main(String args[]) throws CloneNotSupportedException {

        // game loop
        while (true) {

            Scanner in = new Scanner(System.in);

            ArrayList<Spell> mySpells = new ArrayList<>();
            ArrayList<Spell> opSpells = new ArrayList<>();
            ArrayList<Spell> tomeSpells = new ArrayList<>();
            ArrayList<Potion> potions = new ArrayList<>();

            int actionCount = in.nextInt(); // the number of spells and recipes in play
            long start = System.currentTimeMillis();
            for (int i = 0; i < actionCount; i++) {
                int actionId = in.nextInt(); // the unique ID of this spell or recipe
                String actionType = in.next(); // in the first league: BREW; later: CAST, OPPONENT_CAST, LEARN, BREW
                int delta0 = in.nextInt(); // tier-0 ingredient change
                int delta1 = in.nextInt(); // tier-1 ingredient change
                int delta2 = in.nextInt(); // tier-2 ingredient change
                int delta3 = in.nextInt(); // tier-3 ingredient change
                int[] delta = {delta0, delta1, delta2, delta3};
                int price = in.nextInt(); // the price in rupees if this is a potion
                int tomeIndex = in.nextInt(); // in the first two leagues: always 0; later: the index in the tome if this is a tome spell, equal to the read-ahead tax; For brews, this is the value of the current urgency bonus
                int taxCount = in.nextInt(); // in the first two leagues: always 0; later: the amount of taxed tier-0 ingredients you gain from learning this spell; For brews, this is how many times you can still gain an urgency bonus
                boolean castable = in.nextInt() != 0; // in the first league: always 0; later: 1 if this is a castable player spell
                boolean repeatable = in.nextInt() != 0; // for the first two leagues: always 0; later: 1 if this is a repeatable player spell
                if (actionType.equals("CAST"))
                    mySpells.add(new Spell(actionId, delta, castable, repeatable));
                else if (actionType.equals("OPPONENT_CAST"))
                    opSpells.add(new Spell(actionId, delta, castable, repeatable));
                else if (actionType.equals("BREW"))
                    potions.add(new Potion(actionId, delta, price, tomeIndex));
                else if (actionType.equals("LEARN"))
                    tomeSpells.add(new Spell(actionId, delta, castable, repeatable));
            }
            int[] myInv = new int[4];
            int[] opInv = new int[4];
            int myScore, opScore;
            for (int i = 0; i < 2; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees
                if (i == 0) {
                    // System.err.println("my inventory: " + new int[] {inv0, inv1, inv2, inv3});
                    myInv = new int[]{inv0, inv1, inv2, inv3};
                    myScore = score;
                } else {
                    opInv = new int[]{inv0, inv1, inv2, inv3};
                    opScore = score;
                }
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            System.err.println("my inventory: " + Arrays.toString(myInv));

            Action action = new Action();
            // IA For dummies
            if (mySpells.size() < MAX_SPELLS) {
                System.err.println("LEARN SPELL");
                action.type = "LEARN";
                action.id = tomeSpells.get(0).id;
            } else {
                int bestPrice = 0;
                Potion bestPotion = null;
                for (Potion p : potions) {
                    if (canBrew(myInv, p) && (p.price + p.bonus) > bestPrice) {
                        bestPotion = p;
                        bestPrice = p.price + p.bonus;
                    }
                }
                if (bestPotion != null) {
                    System.err.println("BREW POTION");
                    action.type = "BREW";
                    action.id = bestPotion.id;
                } else {
                    System.err.println("CAST SPELL");
                    ArrayList<Potion> potionsCandidates = new ArrayList<>();
                    for (Potion p : potions) {
                        if (norm(somme(p.delta, myInv)) <= norm(somme(p.delta, opInv)))
                            potionsCandidates.add(p);
                    }
                    if (potionsCandidates.size() == 0)
                        potionsCandidates = potions;
                    double maxDist = Double.POSITIVE_INFINITY;
                    bestPotion = null;
                    for (Potion p : potionsCandidates) {
                        int dist = p.evalDist(myInv);
                        if (dist < maxDist) {
                            bestPotion = p;
                            maxDist = dist;
                        }
                    }
                    System.err.println("player's inventory " + Arrays.toString(myInv) + " - nearest potion = " + bestPotion + " - dist = " + bestPotion.evalDist(myInv));
                    Action lastAction = null;
                    Node root = new Node(myInv, mySpells, lastAction);
                    root.parent = null;
                    Node resultNode = Bfs(root, bestPotion);
                    Node node = resultNode;
                    if (node != null) {
                        while (node.parent.parent != null) {
                            node = node.parent;
                        }
                        System.err.println("BFS OK! Next action = [" + node.lastAction + "]");
                        System.err.println(String.format("path to potion (%s)\n%s", bestPotion, resultNode));
                        action = node.lastAction;
                    } else
                        action = new Action("REST");
                }
            }
            long elapsedTime = System.currentTimeMillis() - start;
            System.err.println("Elapsed time = " + Math.round(elapsedTime) + " ms");
            System.err.println("nodesCount = " + nodesCount);

            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT
            System.out.println(action);

            System.gc();
        }
    }
}
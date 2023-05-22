import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

public class lab5 {

    public static int pc = 0;
    public static HashMap<String, Integer> registers = new HashMap<String, Integer>();
    public static int[] data_memory = new int[8192];
    public static HashMap<String, String> instructType = new HashMap<String, String>();
    public static ArrayList<String> instructions = new ArrayList<String>();
    public static HashMap<String, Integer> labels = new HashMap<String, Integer>();
    public static ArrayList<Instruction> instructionObject = new ArrayList<Instruction>();

    public static LinkedList<Integer> GHR = new LinkedList<Integer>();
    public static int[] selector;
    public static int correctPrediction;
    public static int savePC;
    public static int taken;

    public static String[][] registerNames = { { "$0", "$v0", "$v1", "$a0" },
            { "$a1", "$a2", "$a3", "$t0" },
            { "$t1", "$t2", "$t3", "$t4" },
            { "$t5", "$t6", "$t7", "$s0" },
            { "$s1", "$s2", "$s3", "$s4" },
            { "$s5", "$s6", "$s7", "$t8" },
            { "$t9", "$sp", "$ra" } };

    public static void printRegisters() {
        System.out.println("pc = " + pc);

        for (int i = 0; i < registerNames.length; i++) {
            String line = "";
            for (int j = 0; j < registerNames[i].length; j++) {
                String reg_str = registerNames[i][j] + " = " + registers.get(registerNames[i][j]);
                line += String.format("%-15s", reg_str);
            }
            System.out.println(line);
        }
    }

    public static void fillRegister(HashMap<String, Integer> inputMap) {
        inputMap.put("$zero", 0);
        inputMap.put("$0", 0);
        inputMap.put("$v0", 0);
        inputMap.put("$v1", 0);
        inputMap.put("$a0", 0);
        inputMap.put("$a1", 0);
        inputMap.put("$a2", 0);
        inputMap.put("$a3", 0);
        inputMap.put("$t0", 0);
        inputMap.put("$t1", 0);
        inputMap.put("$t2", 0);
        inputMap.put("$t3", 0);
        inputMap.put("$t4", 0);
        inputMap.put("$t5", 0);
        inputMap.put("$t6", 0);
        inputMap.put("$t7", 0);
        inputMap.put("$t8", 0);
        inputMap.put("$t9", 0);
        inputMap.put("$s0", 0);
        inputMap.put("$s1", 0);
        inputMap.put("$s2", 0);
        inputMap.put("$s3", 0);
        inputMap.put("$s4", 0);
        inputMap.put("$s5", 0);
        inputMap.put("$s6", 0);
        inputMap.put("$s7", 0);
        inputMap.put("$ra", 0);
        inputMap.put("$sp", 0);

    }

    public static void fillType(HashMap<String, String> inputMap) {
        inputMap.put("and", "r");
        inputMap.put("or", "r");
        inputMap.put("add", "r");
        inputMap.put("addi", "i");
        inputMap.put("sll", "r");
        inputMap.put("sub", "r");
        inputMap.put("slt", "r");
        inputMap.put("beq", "i");
        inputMap.put("bne", "i");
        inputMap.put("lw", "i");
        inputMap.put("sw", "i");
        inputMap.put("j", "j");
        inputMap.put("jr", "r");
        inputMap.put("jal", "j");

    }

    public static void parseASM(Scanner scanner) {
        fillRegister(registers);
        fillType(instructType);

        /* first pass to find labels and get instructions */
        int count = 0;
        while (scanner.hasNext()) {
            String line = scanner.nextLine().trim();

            // skip if empty line or comment
            if (line.length() == 0 || line.charAt(0) == '#')
                continue;

            // remove comment from line
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#")).trim();
            }

            // add to label and instruction list
            if (line.contains(":")) {
                String[] split_label = line.split(":");
                labels.put(split_label[0], count);
                if (split_label.length == 1)
                    continue;
                else
                    instructions.add(split_label[1].trim());
            } else {
                instructions.add(line);
            }
            count++;
        }
        scanner.close();

        for (int i = 0; i < instructions.size(); i++) {

            String first = instructions.get(i).replaceAll("[()]", "");
            String before = first.replace("$", " $");
            String splity = before.replace(",", " ");
            String after = splity.trim().replaceAll(" +", " ");
            String[] split_instruct = after.split(" ");

            String op = split_instruct[0].replaceAll("\\s", "");

            String val1 = split_instruct[1];

            String type = instructType.get(op);

            if (type == "r") {
                if (op.equals("jr")) {
                    instructionObject.add(new Instruction("r", op, val1, "n/a", "n/a"));
                } else if (op.equals("sll")) {
                    String val2 = split_instruct[2];
                    String val3 = split_instruct[3];
                    instructionObject.add(new Instruction("r", op, val2, val3, val1));
                } else {
                    String val2 = split_instruct[2];
                    String val3 = split_instruct[3];
                    instructionObject.add(new Instruction("r", op, val2, val3, val1));
                }

            } else if (type == "i") {

                String val2 = split_instruct[2];
                String val3 = split_instruct[3];

                if (op.equals("lw") || op.equals("sw")) {
                    int imd = Integer.parseInt(val2);
                    instructionObject.add(
                            new Instruction("i", op, val3, val1, imd));

                } else if (op.equals("bne") || op.equals("beq")) {
                    int imd = labels.get(val3);
                    instructionObject.add(
                            new Instruction("i", op, val2, val1, imd));
                } else {
                    int imd = Integer.parseInt(val3);
                    instructionObject.add(
                            new Instruction("i", op, val2, val1, imd));
                }
            } else if (type == "j") {
                int imd = Integer.parseInt(Integer.toString(labels.get(val1)));
                instructionObject.add(new Instruction("j", op, imd));
            } else {
                instructionObject.add(new Instruction("invalid", op));
                break;
            }
        }

    }

    public static void correlatingPredictor(int result){
        int sum = 0;
        for(int i = GHR.size() - 1; i > 0; i--){
            sum += sum + Math.pow(2, GHR.get(i));
        }

        if(selector[sum] == 2 || selector[sum] == 3){
            if(result == 1){
                if(selector[sum] == 2){
                    selector[sum] += 1;
                }
                correctPrediction ++;
            }
            else{
                selector[sum] -= 1;
            }
        }
        else if (selector[sum] == 0 || selector[sum] == 1) {
            if(result == 0){
                if(selector[sum] == 1){
                    selector[sum] -= 1;
                }
                correctPrediction ++;
            }
            else{
                selector[sum] += 1;
            }

        }

        GHR.removeFirst();
        GHR.addLast(result);

    }

    public static void handleInstruct(Instruction instruct) {

        String op = instruct.opcode;
        int temp;
        switch (op) {
            case "and":
                temp = (registers.get(instruct.rs) & registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "or":
                temp = (registers.get(instruct.rs) | registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "add":
                temp = (registers.get(instruct.rs) + registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "addi":
                temp = (registers.get(instruct.rs) + instruct.immediate);
                registers.put(instruct.rt, temp);
                pc++;
                break;
            case "sll":
                temp = (registers.get(instruct.rs) << Integer.parseInt(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "sub":
                temp = (registers.get(instruct.rs) - registers.get(instruct.rt));
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "slt":
                if (registers.get(instruct.rs) < registers.get(instruct.rt)) {
                    temp = 1;
                } else {
                    temp = 0;
                }
                registers.put(instruct.rd, temp);
                pc++;
                break;
            case "beq":
                savePC = pc;
                if (registers.get(instruct.rt) == registers.get(instruct.rs)) {
                    pc = instruct.immediate;
                } else {
                    pc++;
                }

                if(savePC + 1 == pc){
                    taken = 0;
                }
                else{
                    taken = 1;
                }

                correlatingPredictor(taken);
                break;

            case "bne":
                savePC = pc;
                if (registers.get(instruct.rt) != registers.get(instruct.rs)) {
                    pc = instruct.immediate;
                } else {
                    pc++;
                }

                if(savePC + 1 == pc){
                    taken = 0;
                }
                else{
                    taken = 1;
                }

                correlatingPredictor(taken);
                break;
            case "lw":
                int mem = data_memory[instruct.immediate + registers.get(instruct.rs)];
                registers.put(instruct.rt, mem);
                pc++;
                break;
            case "sw":
                data_memory[instruct.immediate + registers.get(instruct.rs)] = registers.get(instruct.rt);
                pc++;
                break;
            case "j":
                pc = instruct.address;
                break;
            case "jal":
                registers.put("$ra", pc + 1);
                pc = instruct.address;
                break;
            case "jr":
                pc = registers.get(instruct.rs);
                break;
            default:
                System.exit(0);

        }

    }

    public static void completeOperation() {

        Instruction instruct;

        while (pc < instructionObject.size()) {
            handleInstruct(instructionObject.get(pc));
        }
    }

    public static void handleCommand(String[] input) {
        String command = input[0];

        if (command.equals("q")) {
            System.exit(0);

        } else if (command.equals("h")) {
            String help_msg = "h = show help\n";
            help_msg += "d = dump register state\n";
            help_msg += "s = single step through the program (i.e. execute 1 instruction and stop)\n";
            help_msg += "s num = step through num instructions of the program\n";
            help_msg += "r = run until the program ends\n";
            help_msg += "m num1 num2 = display data memory from location num1 to num2\n";
            help_msg += "c = clear all registers, memory, and the program counter to 0\n";
            help_msg += "q = exit the program";
            System.out.println(help_msg);

        } else if (command.equals("d")) {
            System.out.println("");
            printRegisters();

        } else if (command.equals("s")) {
            if (input.length == 2) {
                String count = input[1];
                System.out.printf("%9s instruction(s) executed\n", count);
                for (int i = 0; i < Integer.parseInt(count); i++) {
                    handleInstruct(instructionObject.get(pc));
                }
            } else if (input.length == 1) {
                System.out.println("        1 instruction(s) executed");
                handleInstruct(instructionObject.get(pc));
            } else {
                System.out.println("        Incorrect number of arguments for command s");
            }

        } else if (command.equals("r")) {
            completeOperation();

        } else if (command.equals("m")) {
            if (input.length != 3) {
                System.out.println("        Incorrect number of arguments for command m");
                return;
            }
            System.out.println("");
            int num1 = Integer.parseInt(input[1]);
            int num2 = Integer.parseInt(input[2]);
            for (int i = num1; i <= num2; i++) {
                System.out.println("[" + i + "] = " + data_memory[i]);
            }

        } else if (command.equals("c")) {
            System.out.println("        Simulator reset");
            registers.replaceAll((k, v) -> 0);
            pc = 0;
            Arrays.fill(data_memory, 0);

        } else {
            System.out.println("Incorrect command.\n");

        }
    }

    public static void main(String[] args) throws FileNotFoundException {

        File file = new File(args[0]);
        Scanner scanner = new Scanner(file);

        parseASM(scanner);

        String mode = "";
        switch (args.length) {
            case 1:
                mode = "interactive";
                break;
            case 2:
                mode = "script";
                break;
            default:
                System.out.println("Incorrect number of arguments");
                System.exit(0);
        }

        String[] input;
        if (mode == "interactive") {

            Scanner scanner2 = new Scanner(System.in);
            Boolean quit = false;
            do {
                System.out.print("mips> ");
                input = scanner2.nextLine().split("\\s");
                handleCommand(input);
                System.out.println("");
            } while (!quit);
            scanner2.close();

        }
        if (mode == "script") {
            Scanner scanner2 = new Scanner(new File(args[1]));
            while (scanner2.hasNext()) {
                String line = scanner2.nextLine();
                input = line.split("\\s");
                System.out.println("mips> " + line);
                handleCommand(input);
                System.out.println("");
            }
            scanner2.close();
        }
        scanner.close();
    }
}

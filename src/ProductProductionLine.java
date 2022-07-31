import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ProductProductionLine {
    private static final int TOTAL_UNIT = 12;   // total units to produce
    private static final int TIME_A = 1000; // time needed to produce component A
    private static final int TIME_B = 2000; // time needed to produce component B
    private static final int TIME_ASSEMBLE = 2000; // time needed to assemble components A and B as a product
    private static final int TIME_PACK = 2000; // time needed to pack 6 products into a box
    private static final int TIME_LABEL = 500; // time needed to label a box

    private static final Semaphore assembler = new Semaphore(1); // 1 assembler
    private static final Semaphore packer = new Semaphore(1); // 1 packer
    private static final Semaphore labeller = new Semaphore(1); // 1 labeller

    private static final Semaphore productionA = new Semaphore(1); // 1 component A produced at once
    private static final Semaphore productionB = new Semaphore(1); // 1 component B produced at once

    private static final Semaphore componentA = new Semaphore(0); // check availability of component A
    private static final Semaphore componentB = new Semaphore(0); // check availability of component B

    private static final Semaphore product = new Semaphore(0); // check if a product is ready to assemble
    private static final Semaphore box = new Semaphore(0); // check if a box is ready to label

    private static int unitNumberA = 0; // initialisation: to keep tract number of components A produced
    private static int unitNumberB = 0; // initialisation: to keep tract number of components B produced
    private static int productNumber = 0; // initialisation: to keep tract number of products assembled
    private static int boxNumber = 0; // initialisation: to keep tract number of boxes packed

//     A thread to simulate the production of Component-A.
    public static class componentA implements Runnable {

        @Override
        public void run() {
            try {
                productionA.acquire();  // 1 unit of component A is producing, productionA.availablePermits() = 0
                Thread.sleep(TIME_A); // wait to produce component A
                unitNumberA++; // 1 unit of component A is produced
                System.out.println("        Component-A: Unit-" + unitNumberA + " produced.");
                componentA.release(); // 1 unit of component A is ready to be assembled, componentA.availablePermits()+1
                productionA.release(); // production A done, productionA.availablePermits() = 1
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

//    A thread to simulate the production of Component-B.
    public static class componentB implements Runnable {

        @Override
        public void run() {
            try {
                productionB.acquire(); // 1 unit of component B is producing, productionB.availablePermits() = 0
                Thread.sleep(TIME_B); // wait to produce component B
                unitNumberB++; // 1 unit of component A is produced
                System.out.println("        Component-B: Unit-" + unitNumberB + " produced.");
                componentB.release(); // 1 unit of component B is ready to be assembled, componentB.availablePermits()+1
                productionB.release(); // production B done, productionB.availablePermits() = 1
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

//    A thread to simulate the Assembler.
    public static class assembler implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Assembler: waiting for components");
                while (true) {
                    if (componentA.availablePermits() >= 1 && componentB.availablePermits() >= 1) {
                        assembler.acquire(); // assembling components A and B, assembler.availablePermits() = 0
                        Thread.sleep(TIME_ASSEMBLE); // wait to assemble component A and B to produce a product
                        productNumber++; // a product is assembled
                        System.out.println("Assembler: Product-" + productNumber + " completed.");
                        if (productNumber % 6 == 0) {
                            product.release(); // 6 products are ready to pack, product.availablePermits()+1
                        }
                        componentA.acquire(); // component A has been assembled, componentA.availablePermits()-1
                        componentB.acquire(); // component B has been assembled, componentB.availablePermits()-1
                        assembler.release(); // done assembling a product, assembler.availablePermits() = 1
                    }
                }

            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

//    A thread to simulate the Packer.
    public static class packer implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Packer: waiting for products");
                while (true) {
                    if (product.availablePermits() >= 1) {
                        packer.acquire(); // packing 6 products, packer.availablePermits() = 0
                        Thread.sleep(TIME_PACK); // wait to pack 6 products into a box
                        boxNumber++; // a box is packed
                        System.out.println("                Packer: Box-" + boxNumber + " completed.");
                        product.acquire(); // product has been packed, product.availablePermits()-1
                        box.release(); // a box is ready to label, box.availablePermits()+1
                        packer.release(); // done packing a box, packer.availablePermits() = 1
                    }
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

//    A thread to simulate the Labeller.
    public static class labeller implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Labeller: waiting for box");
                while (true) {
                    if (box.availablePermits() >= 1) {
                        labeller.acquire(); // labelling a blx, labeller.availablePermits() = 0
                        Thread.sleep(TIME_LABEL); // Time to pack 6 products into a box
                        System.out.println("                Labeller: Box-" + boxNumber + " labelled.");
                        box.acquire(); // a box has been labelled, box.availablePermits()-1
                        labeller.release(); // done labelling a box, labeller.availablePermits() = 1
                    }
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ExecutorService exe = Executors.newCachedThreadPool();

        // the production line
        exe.execute(new assembler());
        exe.execute(new packer());
        exe.execute(new labeller());

        for (int i = 0; i < TOTAL_UNIT; i++) {
            exe.execute(new componentA());
            exe.execute(new componentB());
        }

        exe.shutdown(); // Shut down the executor, but allow the tasks in the executor to complete
    }
}

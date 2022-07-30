import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ProductProductionLine {
    private static final Semaphore assembler = new Semaphore(1); // 1 assembler
    private static final Semaphore packer = new Semaphore(1); // 1 packer
    private static final Semaphore labeller = new Semaphore(1); // 1 labeller

    private static final Semaphore productionA = new Semaphore(1); // 1 component A produced at once
    private static final Semaphore productionB = new Semaphore(1); // 1 component B produced at once

    private static final Semaphore componentA = new Semaphore(0); //
    private static final Semaphore componentB = new Semaphore(0); //

    private static final Semaphore product = new Semaphore(6); //
    private static final Semaphore box = new Semaphore(0); //

    private static int unitNumberA = 0; // initialisation
    private static int unitNumberB = 0; // initialisation
    private static int productNumber = 0; // initialisation
    private static int boxNumber = 0; // initialisation


    public static class componentA implements Runnable {

        @Override
        public void run() {
            try {
                productionA.acquire();
                unitNumberA++;
                Thread.sleep(1000); // Time to produce component A
                System.out.println("Component-A: Unit-" + unitNumberA + " produced.");
                componentA.release();
                productionA.release();
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static class componentB implements Runnable {

        @Override
        public void run() {
            try {
                productionB.acquire();
                unitNumberB++;
                Thread.sleep(2000); // Time to produce component B
                System.out.println("Component-B: Unit-" + unitNumberB + " produced.");
                componentB.release();
                productionB.release();
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static class assembler implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Assembler: waiting for components");
                while(true){
                    if (componentA.availablePermits() >= 1 && componentB.availablePermits() >= 1) {
                        assembler.acquire();
                        productNumber++;
                        Thread.sleep(2000); // Time to assemble component A and B to produce a product
                        System.out.println("Assembler: Product-" + productNumber + " completed.");
                        product.acquire();
                        componentA.acquire();
                        componentB.acquire();
                        assembler.release();
                    }
                }

            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static class packer implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Packer: waiting for products");
                while(true){
                    if (product.availablePermits() == 0) {
                        packer.acquire();
                        boxNumber++;
                        Thread.sleep(2000); // Time to pack 6 products into a box
                        System.out.println("Packer: Box-" + boxNumber + " completed.");
                        product.release();
                        product.release();
                        product.release();
                        product.release();
                        product.release();
                        product.release();
                        box.release();
                        packer.release();
                    }
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static class labeller implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Labeller: waiting for box");
                while(true){
                    if (box.availablePermits() == 1) {
                        labeller.acquire();
                        Thread.sleep(500); // Time to pack 6 products into a box
                        System.out.println("Labeller: Box-" + boxNumber + " labelled.");
                        box.acquire();
                        labeller.release();
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

        for (int i = 0; i < 15; i++) {
            exe.execute(new componentA());
            exe.execute(new componentB());
        }

        exe.shutdown(); // Shut down the executor, but allow the tasks in the executor to complete
    }
}

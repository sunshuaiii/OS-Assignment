import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.*;

public class DemoShipUnloading extends Thread {

    private static Semaphore dock = new Semaphore(2); // make sure only 2 dock
    private static Semaphore unloadDock1 = new Semaphore(0); // enable unloading to 1 train only in dock 1
    private static Semaphore unloadDock2 = new Semaphore(0); // enable unloading to 1 trailer only in dock 2
    private static Semaphore train = new Semaphore(1); //  make sure only 1 train in dock 1
    private static Semaphore trailer = new Semaphore(1); // make sure only 1 trailer in dock 2
    private static Semaphore ship1_isEmpty = new Semaphore(0); // check if the ship 1 is empty
    private static Semaphore ship2_isEmpty = new Semaphore(0); // check if the ship 2 is empty
    private static Semaphore checkGate = new Semaphore(1); // check the gate availability

    private static int dock1CargoCount = 0; // the amount of cargos in the ship parked at dock 1
    private static int dock2CargoCount = 0; // the amount of cargos in the ship parked at dock 2

    //Thread 1: task of Ship
    public static class ship implements Runnable {
        int shipNumber;

        public ship(int shipNumber) {
            this.shipNumber = shipNumber;
        }

        @Override
        public void run() {

            try {

                dock.acquire();      // if a dock is available, the semaphore 2 decrease by 1
                if (train.availablePermits() == 1) {
                    train.acquire();   // if a train is available, the semaphore 1 decrease by 1

                    dock1CargoCount = (int) Math.floor((Math.random() * (30 - 20 + 1) + 20));    // the random number of cargos generated between 20-30 cargos
                    System.out.println("Ship-" + shipNumber + ": Carrying " + dock1CargoCount + " cargos and waiting to park at Dock 1...");
                    unloadDock1.release();	  // enable the train to unload cargo, the semaphore 0 increase by 1

                    System.out.println("Ship-" + shipNumber + ": Unloading...");
                    ship1_isEmpty.acquire();     //all the cargos are unloaded and the ship1 is empty
                    Thread.sleep(1000);		// time to load cargo to train

                    System.out.println("Ship-" + shipNumber + ": Departs");
                    dock.release();    // the ship is departed and  the dock is available for next ship
                }

                else if (trailer.availablePermits() == 1)  {
                    trailer.acquire();   // if a trailer is available, the semaphore 1 decrease by 1

                    dock2CargoCount = (int) Math.floor((Math.random() * (30 - 20 + 1) + 20));    // the random number of cargos generated between 20-30 cargos
                    System.out.println("Ship-" + shipNumber + ": Carrying " + dock2CargoCount + " cargos and waiting to park at Dock 2...");
                    unloadDock2.release();  // enable the trailer to unload cargo, the semaphore 0 increase by 1

                    System.out.println("Ship-" + shipNumber + ": Unloading...");
                    ship2_isEmpty.acquire();   //all the cargos are unloaded and the ship2 is empty
                    Thread.sleep(1000);  // time to load cargo to trailer

                    System.out.println("Ship-" + shipNumber + ": Departs");
                    dock.release();   // the ship is departed and the dock is available for next ship
                }


            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

    }

    //Thread 2 - train task
    public static class trainTask implements Runnable {

        int trainNo;
        int trainCargo = 4; // the amount of cargos that each train can carry

        public trainTask(int trainNo) {
            this.trainNo = trainNo;

        }

        @Override
        public void run() {
            try {
                System.out.println("Train " + trainNo + ": Waiting for ship...");

                while (true) {

                    unloadDock1.acquire();  // 1 train is loading the cargos
                    System.out.println("Train " + trainNo + " (Remaining " + dock1CargoCount + " cargos on ship of dock 1): Loading " + trainCargo + " cargos... ");
                    Thread.sleep(2000); // Time to load cargos to the train
                    dock1CargoCount -= trainCargo; // Reduce the amount of cargos by 4 after loaded into the train

                    if (dock1CargoCount <= 0) {
                        // if the remaining cargos are less than 4, the train will load the remaining cargos and send to storage field
                        ship1_isEmpty.release(); //ship at dock 1 is empty, ship can depart
                        train.release();

                    } else {
                        System.out.println("Train " + trainNo + ": Departing from dock..."); //train loaded and departed
                        Thread.sleep(4000); // Time needed for train to reach the cross section
                        unloadDock1.release(); // the train is departed and release available unloading at the dock for the next train
                    }

                    System.out.println("Train " + trainNo + ": Reaching the cross section...");
                    checkGate.acquire();  // close the gate when train is passing the cross section
                    Thread.sleep(1000); // time of train passing cross section
                    System.out.println("Train " + trainNo + ": Passing the cross section...");
                    checkGate.release();  //  open the gate after the train  passed the cross section and release the gate availability

                    System.out.println("Train " + trainNo + ": Unloading cargos at the storage field...");
                    Thread.sleep(2000);  //time of unloading cargo in storage field


                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    // Thread 3 -trailer task
    public static class trailerTask implements Runnable {

        int trailerNo;
        int trailerCargo = 2;  // the amount of cargos that each trailer can carry

        public trailerTask(int trailerNo) {
            this.trailerNo = trailerNo;
        }

        @Override
        public void run() {
            try {
                System.out.println("Trailer " + trailerNo + ": Waiting for ship...");
                while (true) {
                    unloadDock2.acquire();  // 1 trailer is loading the cargos
                    System.out.println("Trailer " + trailerNo + " (Remaining "  + dock2CargoCount + " cargos on ship of dock 2): Loading " + trailerCargo + " cargos... ");
                    Thread.sleep(1000); // Time to load cargos to trailer
                    dock2CargoCount -= trailerCargo;  // Reduce the amount of cargos by 2 after loaded into the trailer

                    if (dock2CargoCount <= 0) {
                        // if the remaining cargos are less than 2, the trailer will load the remaining cargos and send to storage field
                        ship2_isEmpty.release(); //ship at dock 2 is empty, ship can depart
                        trailer.release();

                    } else {
                        System.out.println("Trailer " + trailerNo + ": Departing from dock..."); //Trailer loaded and departed
                        Thread.sleep(2000); //Time needed for trailer to reach cross section
                        unloadDock2.release();  // the trailer is departed and release available unloading at the dock for the next trailer
                    }


                    System.out.println("Trailer " + trailerNo + ": Reaching the cross section...");

                    while (checkGate.availablePermits() == 0) // if the train is passing the cross section and the gate is closed, the trailer has to wait the gate to be opened after the train pass
                    {
                        System.out.println("Trailer " + trailerNo + ": Waiting gate to be opened...");
                        Thread.sleep(1000); // Time for waiting the gate to open as train need 1 second to pass
                        checkGate.release(); // the train passed the cross section and the gate is opened
                    }
                    System.out.println("Trailer " + trailerNo + ": Passing the cross section...");

                    System.out.println("Trailer " + trailerNo + ": Unloading cargos at the storage field...");
                    Thread.sleep(1000); // time of unloading cargo in storage field

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public static void main(String[] args) {
        // creates a new thread if all the threads in the pool are busy and there are tasks waiting for execution
        ExecutorService exe = Executors.newCachedThreadPool();

        // Execute all the task
        exe.execute(new trainTask(1));
        exe.execute(new trainTask(2));
        exe.execute(new trailerTask(1));
        exe.execute(new trailerTask(2));
        exe.execute(new trailerTask(3));

        for (int i = 1; i<50 ;i++) {

            try {
                exe.execute(new ship(i));
                Thread.sleep((int)Math.random() * (25000 - 20000 + 1) + 20000); // time of each ship arrive
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        exe.shutdown(); // Shut down the executor, but allow the tasks in the executor to complete
    }
}
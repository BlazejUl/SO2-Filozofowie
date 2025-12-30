package pl.buliasz;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class KelnerBezKolejki {

    public static final int N = 5;
    private static final int SIMULATION_DURATION_SECONDS = 30;
    private static volatile boolean running = true;
    private static final Semaphore waiter = new Semaphore(N - 1);
    private static final Semaphore[] forks = new Semaphore[N];
    private static final String[] philosopherStates = new String[N];
    private static final int[] forkOwners = new int[N];
    private static final int[] mealsEaten = new int[N];
    private static final long[] totalWaitingTime = new long[N];
    private static final long simulationStart = System.currentTimeMillis();
    private static final Object PRINT_LOCK = new Object();

    public static void main(String[] args) {

        for (int i = 0; i < N; i++) {
            forks[i] = new Semaphore(1);
        }

        for (int i = 0; i < N; i++) {
            philosopherStates[i] = "MYŚLI";
            forkOwners[i] = -1;
            mealsEaten[i] = 0;
            totalWaitingTime[i] = 0;
        }

        Philosopher[] philosophers = new Philosopher[N];
        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i);
            philosophers[i].start();
        }

        new Thread(() -> {
            try {
                Thread.sleep(SIMULATION_DURATION_SECONDS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            running = false;

            synchronized (PRINT_LOCK) {
                System.out.println("\n=== KONIEC SYMULACJI ===\n");
                printFinalStats();
            }

            System.exit(0);
        }).start();
    }

    static class Philosopher extends Thread {
        private final int id;
        private final int leftFork;
        private final int rightFork;

        Philosopher(int id) {
            this.id = id;
            this.leftFork = id;
            this.rightFork = (id + 1) % N;
        }

        @Override
        public void run() {
            try {
                while (running) {
                    think();
                    if (!running) break;
                    eat();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            setState("MYŚLI");
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 200));
        }

        private void eat() throws InterruptedException {

            long startWait = System.currentTimeMillis();
            setState("CZEKA");
            waiter.acquire();
            forks[leftFork].acquire();
            synchronized (PRINT_LOCK){
                forkOwners[leftFork] = id;
                printTable();
            }
            forks[rightFork].acquire();
            long waitingTime = System.currentTimeMillis() - startWait;

            synchronized (PRINT_LOCK) {
                philosopherStates[id] = "JE";
                mealsEaten[id]++;
                totalWaitingTime[id] += waitingTime;
                forkOwners[rightFork] = id;
                printTable();
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(1500, 2000));
            synchronized (PRINT_LOCK) {
                forkOwners[leftFork] = -1;
                forkOwners[rightFork] = -1;
                philosopherStates[id] = "MYŚLI";
                printTable();
            }

            forks[leftFork].release();
            forks[rightFork].release();
            waiter.release();
        }

        private void setState(String state) {
            synchronized (PRINT_LOCK) {
                philosopherStates[id] = state;
                printTable();
            }
        }
    }

    private static void printTable() {
        StringBuilder sb = new StringBuilder();
        long elapsed = (System.currentTimeMillis() - simulationStart) / 1000;
        sb.append("Czas trwania symulacji: ").append(elapsed).append(" s\n\n");
        String[] p = new String[N];
        for (int i = 0; i < N; i++) {
            String s = philosopherStates[i];
            if ("JE".equals(s)) p[i] = "J";
            else if ("CZEKA".equals(s)) p[i] = "C";
            else p[i] = "M";
        }

        String[] f = new String[N];
        for (int i = 0; i < N; i++) {
            f[i] = (forkOwners[i] == -1) ? "|" : "X";
        }

        sb.append("      P0(").append(p[0]).append(")\n");
        sb.append("   ").append(f[0]).append("         ").append(f[1]).append("\n");
        sb.append(" P4(").append(p[4]).append(")      P1(").append(p[1]).append(")\n");
        sb.append("   ").append(f[4]).append("         ").append(f[2]).append("\n");
        sb.append(" P3(").append(p[3]).append(")      P2(").append(p[2]).append(")\n");
        sb.append("        ").append(f[3]).append("\n");

        sb.append("\nStatystyki:\n");
        for (int i = 0; i < N; i++) {
            long avg = mealsEaten[i] == 0 ? 0 : totalWaitingTime[i] / mealsEaten[i];
            sb.append("  Filozof ").append(i)
                    .append(", łączny czas czekania = ").append(totalWaitingTime[i]).append(" ms")
                    .append(", średni czas = ").append(avg).append(" ms\n");
        }
        System.out.println(sb.toString());
    }

    private static void printFinalStats() {
        System.out.println("STATYSTYKI KOŃCOWE:");

        for (int i = 0; i < N; i++) {
            long avg = mealsEaten[i] == 0 ? 0 : totalWaitingTime[i] / mealsEaten[i];

            System.out.println("  Filozof " + i +
                    ": zjadł " + mealsEaten[i] + " razy" +
                    ", łączny czas czekania = " + totalWaitingTime[i] + " ms" +
                    ", średni czas = " + avg + " ms");
        }

    }
}

package unifi.stlab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.oristool.models.stpn.*;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

public class SteadyStateAnalysis {

        public static void main(String[] args) {

                PetriNet net;
                Marking marking;

                //PoolRejuvenationBuilder builder = new SequentialCoordinatedRejuvenationBuilder();
                //PoolRejuvenationBuilder builder = new ParallelCoordinatedRejuvenationBuilder();
                //UncoordinatedRejuvenationBuilder builder = new UncoordinatedRejuvenationBuilder();


                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
                String formattedDate = now.format(formatter);

                int[] poolSizes = {1, 2, 4, 8};

                Map<Integer,Integer> performabilityBoundMap = new HashMap<>();
                performabilityBoundMap.put(1, 0);
                performabilityBoundMap.put(2, 1);
                performabilityBoundMap.put(4, 2);
                performabilityBoundMap.put(8, 3);


                PoolRejuvenationBuilder uncoordinatedBuilder = new UncoordinatedRejuvenationBuilder();
                PoolRejuvenationBuilder sequentialbuilder = new SequentialCoordinatedRejuvenationBuilder();
                // PoolRejuvenationBuilder parallelbuilder = new ParallelCoordinatedRejuvenationBuilder();

                ArrayList<PoolRejuvenationBuilder> models = new ArrayList<>();
                models.add(uncoordinatedBuilder);
                models.add(sequentialbuilder);
                // models.add(parallelbuilder);

                int leftBound = 5;
                int rightBound = 5000;
                int granularity = 5;


                for (PoolRejuvenationBuilder builder : models) {
                        net = new PetriNet();
                        marking = new Marking();
                        builder.build(net,marking);
                        System.out.println("Modello: " + builder.getNickName());
                        for (int poolSize : poolSizes) {
                                System.out.println("PoolSize: " + poolSize);
                                File file = new File( "experiment-results" + File.separator + builder.getNickName(), "size" + poolSize + "-" + formattedDate + ".csv");
                                String filePath = file.getPath();
                                createCsv(filePath, "trigger,Ko,Rej,Performability" );



                                String unavailabilityReward = builder.getUnavailabilityReward();
                                String unreliabilityReward = builder.getUnreliabilityReward();
                                String performabilityReward = builder.getPerformabilityReward(performabilityBoundMap.get(poolSize));
                                String rewards = unavailabilityReward + ";" + unreliabilityReward + ";" + performabilityReward;

                                RegSteadyState analysis = RegSteadyState.builder().build();

                                SteadyStateSolution<Marking> result;
                                SteadyStateSolution<RewardRate> resultReward;
                                Map<RewardRate, BigDecimal> steadyState;

                                for(int waitTrigger = leftBound; waitTrigger<= rightBound; waitTrigger+=granularity){

                                        builder.changePoolsize(net,marking, poolSize);
                                        builder.changeTrigger(net,Integer.toString(waitTrigger));

                                        result = analysis.compute(net, marking);
                                        resultReward = SteadyStateSolution.computeRewards(result, rewards);
                                        steadyState = resultReward.getSteadyState();

                                        BigDecimal reliability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals(unreliabilityReward)).findFirst().get().getValue();
                                        //BigDecimal availability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals("Ko+Rej")).findFirst().get().getValue();
                                        BigDecimal availability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals(unavailabilityReward)).findFirst().get().getValue();
                                        BigDecimal performability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals(performabilityReward)).findFirst().get().getValue();

                                        addRow(filePath, waitTrigger, reliability, availability, performability);

                                }

                                System.out.println("Fine PoolSize: " + poolSize);
                        }
                        System.out.println("Fine Modello: " + builder.getNickName());
                }

        }


        public static void createCsv(String filePath, String cols) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                        // Scrive l'intestazione del file CSV
                        writer.write(cols);
                        writer.newLine();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public static void addRow(String filePath,int col0, BigDecimal col1, BigDecimal col2, BigDecimal col3) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                        // Aggiunge una nuova riga al file CSV
                        writer.write(col0 + "," + col1.toString() + "," + col2.toString() + "," + col3.toString());
                        writer.newLine();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}


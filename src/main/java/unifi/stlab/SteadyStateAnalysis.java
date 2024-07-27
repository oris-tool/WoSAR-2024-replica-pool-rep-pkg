package unifi.stlab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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


                PoolRejuvenationBuilder uncoordinatedBuilder = new UncoordinatedRejuvenationBuilder();
                PoolRejuvenationBuilder sequentialbuilder = new SequentialCoordinatedRejuvenationBuilder();
                PoolRejuvenationBuilder parallelbuilder = new ParallelCoordinatedRejuvenationBuilder();

                ArrayList<PoolRejuvenationBuilder> models = new ArrayList<>();
                models.add(uncoordinatedBuilder);
                models.add(sequentialbuilder);
                models.add(parallelbuilder);

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
                                createCsv(filePath, "trigger,Ko,Rej" );



                                String unavailabilityReward = builder.getUnavailabilityReward();
                                String unreliabilityReward = builder.getUnreliabilityReward();
                                String rewards = unavailabilityReward + ";" + unreliabilityReward;

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

                                        addRow(filePath, waitTrigger, reliability, availability);

                                }

                                System.out.println("Fine PoolSize: " + poolSize);
                        }
                        System.out.println("Fine Modello: " + builder.getNickName());
                }

        }




//                String filePath =  builder.getNickName() + "SteadyState-triggerSearch" + formattedDate + ".csv";
//
//
//                createCsv(filePath, "trigger,Ko,Rej" );
//
//
//
//                //String rewards = "Ko;Ko+Rej";
//                String unavailabilityReward = builder.getUnavailabilityReward();
//                String unreliabilityReward = builder.getUnreliabilityReward();
//                //String unreliabilityReward = builder.getModulatedUnreliabilityReward(2);
//                //String unreliabilityReward = builder.getNumberedUnreliabilityReward(2);
//
//
//                String rewards = unavailabilityReward + ";" + unreliabilityReward;
//                //String rewards = "Ko;Ko+RejPool";
//
//
//                RegSteadyState analysis = RegSteadyState.builder().build();
//
//
//                SteadyStateSolution<Marking> result;
//                SteadyStateSolution<RewardRate> resultReward;
//                Map<RewardRate, BigDecimal> steadyState;
//
//
//                for(int waitTrigger = 100; waitTrigger<=10000; waitTrigger+=100){
//
//                        builder.changeTrigger(net,Integer.toString(waitTrigger));
//                        result = analysis.compute(net, marking);
//                        resultReward = SteadyStateSolution.computeRewards(result, rewards);
//                        steadyState = resultReward.getSteadyState();
//
//                        BigDecimal reliability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals(unreliabilityReward)).findFirst().get().getValue();
//                        //BigDecimal availability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals("Ko+Rej")).findFirst().get().getValue();
//                        BigDecimal availability = steadyState.entrySet().stream().filter(t -> t.getKey().toString().equals(unavailabilityReward)).findFirst().get().getValue();
//
//                        addRow(filePath, waitTrigger, reliability, availability);
//
//                        System.out.println("Steady-state probabilities:");
//                        for ( RewardRate m : steadyState.keySet()) {
//                                System.out.printf("%1.6f -- %s%n", steadyState.get(m), m);
//                        }
//
//                }


//        public static void changeTrigger(PetriNet net, String triggerValue){
//                Transition trigger = net.getTransition("trigger");
//                trigger.removeFeature(StochasticTransitionFeature.class);
//                trigger.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(triggerValue), MarkingExpr.from("1", net)));
//        }


        public static void createCsv(String filePath, String cols) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                        // Scrive l'intestazione del file CSV
                        writer.write(cols);
                        writer.newLine();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public static void addRow(String filePath,int col0, BigDecimal col1, BigDecimal col2) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                        // Aggiunge una nuova riga al file CSV
                        writer.write(col0 + "," + col1.toString() + "," + col2.toString());
                        writer.newLine();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }


//                // transient until time=12, error 0.005 (per epoch), integration step=0.02
//                RegTransient analysis = RegTransient.builder()
//                        .greedyPolicy(new BigDecimal("10"), new BigDecimal("0"))
//                        .timeStep(new BigDecimal("0.05")).build();
//
//                TransientSolution<DeterministicEnablingState, Marking> solution =
//                        analysis.compute(net, marking);
//
//
//                String rewards = "Rej;Ok;Err;Ko";
//                TransientSolution<DeterministicEnablingState, RewardRate> solutionRewards = TransientSolution.computeRewards(false, solution, rewards);
//
//                // display transient probabilities
//                new TransientSolutionViewer(solutionRewards);
        //double[][][] solutionArray = solutionRewards.getSolution();
}


package unifi.stlab;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

public class SequentialCoordinatedRejuvenationBuilder implements PoolRejuvenationBuilder {

    @Override
    public void build(PetriNet net, Marking marking) {

        //Generating Nodes
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Rej = net.addPlace("Rej");
        Place RejChoice = net.addPlace("RejChoice");
        Place Triggered = net.addPlace("Triggered");
        Place WaitTrigger = net.addPlace("WaitTrigger");
        Transition err = net.addTransition("err");
        Transition fail = net.addTransition("fail");
        Transition noRej = net.addTransition("noRej");
        Transition rejChoosen = net.addTransition("rejChoosen");
        Transition rejErr = net.addTransition("rejErr");
        Transition rejOk = net.addTransition("rejOk");
        Transition rejuvenate = net.addTransition("rejuvenate");
        Transition repair = net.addTransition("repair");
        Transition trigger = net.addTransition("trigger");

        //Generating Connectors
        net.addInhibitorArc(Ko, rejuvenate);
        net.addInhibitorArc(Ko, trigger);
        net.addPrecondition(RejChoice, noRej);
        net.addPrecondition(Ok, rejOk);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(Triggered, rejOk);
        net.addPrecondition(Err, rejErr);
        net.addPrecondition(Ok, err);
        net.addPostcondition(rejuvenate, Ok);
        net.addPrecondition(Rej, rejuvenate);
        net.addPrecondition(Ko, repair);
        net.addPostcondition(rejChoosen, Triggered);
        net.addPostcondition(rejOk, Rej);
        net.addPostcondition(rejuvenate, RejChoice);
        net.addPrecondition(Triggered, rejErr);
        net.addPrecondition(Err, fail);
        net.addPrecondition(WaitTrigger, trigger);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(trigger, RejChoice);
        net.addPostcondition(err, Err);
        net.addPrecondition(RejChoice, rejChoosen);
        net.addPostcondition(noRej, WaitTrigger);
        net.addPostcondition(rejErr, Rej);

        //Generating Properties
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 8);
        marking.setTokens(Rej, 0);
        marking.setTokens(RejChoice, 0);
        marking.setTokens(Triggered, 0);
        marking.setTokens(WaitTrigger, 1);
        err.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004166*Ok", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.000463*Err", net)));
        noRej.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.5*Ok+0.3", net)));
        noRej.addFeature(new Priority(0));
        rejChoosen.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.3+3*Err", net)));
        rejChoosen.addFeature(new Priority(0));
        rejErr.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1-Err*0.1^Ok", net)));
        rejErr.addFeature(new Priority(0));
        rejOk.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("Err*0.1^Ok", net)));
        rejOk.addFeature(new Priority(0));
        List<GEN> rejuvenate_gens = new ArrayList<>();

        DBMZone rejuvenate_d_0 = new DBMZone(new Variable("x"));
        Expolynomial rejuvenate_e_0 = Expolynomial.fromString("4.28 * Exp[-3.17 x]");
        rejuvenate_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("0.426906"));
        rejuvenate_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("0"));
        GEN rejuvenate_gen_0 = new GEN(rejuvenate_d_0, rejuvenate_e_0);
        rejuvenate_gens.add(rejuvenate_gen_0);

        PartitionedGEN rejuvenate_pFunction = new PartitionedGEN(rejuvenate_gens);
        StochasticTransitionFeature rejuvenate_feature = StochasticTransitionFeature.of(rejuvenate_pFunction);
        rejuvenate.addFeature(rejuvenate_feature);

        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("2*Ko", net)));
        trigger.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("100"), MarkingExpr.from("1", net)));
        trigger.addFeature(new Priority(0));
    }

    @Override
    public void changeTrigger(PetriNet net, String triggerValue) {
        Transition trigger = net.getTransition("trigger");
        trigger.removeFeature(StochasticTransitionFeature.class);
        trigger.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(triggerValue), MarkingExpr.from("1", net)));
    }

    @Override
    public void changePoolsize(PetriNet net, Marking marking, int poolSize) {
        Place ok = net.getPlace("Ok");
        marking.setTokens(ok, poolSize);
    }


    @Override
    public String getUnavailabilityReward(){
        return "Rej";
    }

    @Override
    public String getUnreliabilityReward(){
        return "Ko";
    }

    @Override
    public String getModulatedUnreliabilityReward(int module){
        return "Ko/" + module;
    }

    @Override
    public String getNumberedUnreliabilityReward(int module) {
        return "Ko>=" + module;
    }

    @Override
    public String getNickName() {
        return "coordinated-sequential";
    }


}

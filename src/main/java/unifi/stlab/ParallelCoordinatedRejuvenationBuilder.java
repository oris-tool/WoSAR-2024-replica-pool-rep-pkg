package unifi.stlab;

import java.math.BigDecimal;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

public class ParallelCoordinatedRejuvenationBuilder implements PoolRejuvenationBuilder {

    @Override
    public void build(PetriNet net, Marking marking) {

        //Generating Nodes
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place RejPool = net.addPlace("RejPool");
        Place RejSelection = net.addPlace("RejSelection");
        Place Triggered = net.addPlace("Triggered");
        Place WaitTrigger = net.addPlace("WaitTrigger");
        Place rejChoice = net.addPlace("rejChoice");
        Transition beginRej = net.addTransition("beginRej");
        Transition err = net.addTransition("err");
        Transition fail = net.addTransition("fail");
        Transition noRej = net.addTransition("noRej");
        Transition rejChoosen = net.addTransition("rejChoosen");
        Transition rejErr = net.addTransition("rejErr");
        Transition rejOk = net.addTransition("rejOk");
        Transition rejuvenate = net.addTransition("rejuvenate");
        Transition repair = net.addTransition("repair");
        Transition selectAnother = net.addTransition("selectAnother");
        Transition trigger = net.addTransition("trigger");

        //Generating Connectors
        net.addInhibitorArc(Ko, trigger);
        net.addInhibitorArc(RejPool, trigger);
        net.addInhibitorArc(Ko, rejuvenate);
        net.addPrecondition(rejChoice, noRej);
        net.addPrecondition(Ko, repair);
        net.addPostcondition(trigger, rejChoice);
        net.addPrecondition(RejSelection, selectAnother);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(RejSelection, beginRej);
        net.addPrecondition(Triggered, rejErr);
        net.addPostcondition(selectAnother, Triggered);
        net.addPrecondition(WaitTrigger, trigger);
        net.addPostcondition(rejErr, RejSelection);
        net.addPrecondition(rejChoice, rejChoosen);
        net.addPostcondition(rejOk, RejSelection);
        net.addPostcondition(noRej, WaitTrigger);
        net.addPostcondition(rejuvenate, Ok);
        net.addPrecondition(RejPool, rejuvenate);
        net.addPrecondition(Err, fail);
        net.addPostcondition(beginRej, WaitTrigger);
        net.addPostcondition(rejChoosen, Triggered);
        net.addPrecondition(Triggered, rejOk);
        net.addPrecondition(Err, rejErr);
        net.addPrecondition(Ok, err);
        net.addPostcondition(beginRej, RejPool);
        net.addPostcondition(err, Err);
        net.addPostcondition(selectAnother, RejPool);
        net.addPostcondition(repair, Ok);
        net.addPrecondition(Ok, rejOk);

        //Generating Properties
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 8);
        marking.setTokens(RejPool, 0);
        marking.setTokens(RejSelection, 0);
        marking.setTokens(Triggered, 0);
        marking.setTokens(WaitTrigger, 1);
        marking.setTokens(rejChoice, 0);
        beginRej.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("Ok+0.3", net)));
        beginRej.addFeature(new Priority(0));
        err.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004166*Ok", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.000463*Err", net)));
        noRej.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.5*Ok+0.3", net)));
        noRej.addFeature(new Priority(0));
        rejChoosen.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.3+3*Err", net)));
        rejChoosen.addFeature(new Priority(0));
        rejErr.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("max(1-Err*0.1^Ok,0.0000001)", net)));
        rejErr.addFeature(new Priority(0));
        rejOk.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("max(Err*0.1^Ok,0.00000001)", net)));
        rejOk.addFeature(new Priority(0));
        rejuvenate.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("RejPool*6", net)));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("2*Ko", net)));
        selectAnother.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.3+6*Err", net)));
        selectAnother.addFeature(new Priority(0));
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
    public String getUnavailabilityReward() {
        return "RejPool";
    }

    @Override
    public String getUnreliabilityReward() {
        return "Ko";
    }

    @Override
    public String getModulatedUnreliabilityReward(int module) {
        return "Ko/" + module;
    }

    @Override
    public String getNickName() {
        return "coordinated-parallel";
    }

    @Override
    public String getNumberedUnreliabilityReward(int module) {
        return "Ko>=" + module;
    }


}

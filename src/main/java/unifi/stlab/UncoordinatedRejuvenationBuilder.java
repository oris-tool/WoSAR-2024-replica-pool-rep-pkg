package unifi.stlab;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import java.math.BigDecimal;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;


public class UncoordinatedRejuvenationBuilder implements PoolRejuvenationBuilder {

    @Override
    public void build(PetriNet net, Marking marking) {
        //Generating Nodes
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place RejErrChoice = net.addPlace("RejErrChoice");
        Place RejOkChoice = net.addPlace("RejOkChoice");
        Place RejPool = net.addPlace("RejPool");
        Transition err = net.addTransition("err");
        Transition fail = net.addTransition("fail");
        Transition noRejErr = net.addTransition("noRejErr");
        Transition noRejOk = net.addTransition("noRejOk");
        Transition rejErr = net.addTransition("rejErr");
        Transition rejErrTrigger = net.addTransition("rejErrTrigger");
        Transition rejOk = net.addTransition("rejOk");
        Transition rejOkTrigger = net.addTransition("rejOkTrigger");
        Transition rejuvenate = net.addTransition("rejuvenate");
        Transition repair = net.addTransition("repair");

        //Generating Connectors
        net.addPrecondition(Ok, err);
        net.addPrecondition(Ko, repair);
        net.addPrecondition(Err, fail);
        net.addPrecondition(RejPool, rejuvenate);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(err, Err);
        net.addPrecondition(Err, rejErrTrigger);
        net.addPostcondition(rejuvenate, Ok);
        net.addPrecondition(Ok, rejOkTrigger);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(RejErrChoice, rejErr);
        net.addPostcondition(rejErr, RejPool);
        net.addPrecondition(RejOkChoice, rejOk);
        net.addPostcondition(rejOk, RejPool);
        net.addPrecondition(RejErrChoice, noRejErr);
        net.addPrecondition(RejOkChoice, noRejOk);
        net.addPostcondition(rejErrTrigger, RejErrChoice);
        net.addPostcondition(rejOkTrigger, RejOkChoice);
        net.addPostcondition(noRejOk, Ok);
        net.addPostcondition(noRejErr, Err);

        //Generating Properties
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 8);
        marking.setTokens(RejErrChoice, 0);
        marking.setTokens(RejOkChoice, 0);
        marking.setTokens(RejPool, 0);
        err.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004166*Ok", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.000463*Err", net)));
        noRejErr.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.1", net)));
        noRejErr.addFeature(new Priority(0));
        noRejOk.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.9", net)));
        noRejOk.addFeature(new Priority(0));
        rejErr.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.9", net)));
        rejErr.addFeature(new Priority(0));
        rejErrTrigger.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.01*Err", net)));
        rejOk.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.1", net)));
        rejOk.addFeature(new Priority(0));
        rejOkTrigger.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.01*Ok", net)));
        rejuvenate.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("RejPool*6", net)));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("2*Ko", net)));
    }

    @Override
    public void changeTrigger(PetriNet net, String triggerValue) {
        double trigger = Double.parseDouble(triggerValue);
        trigger = 1/trigger;

        triggerValue = new BigDecimal(trigger).toPlainString();

        Transition okTrigger = net.getTransition("rejOkTrigger");
        okTrigger.removeFeature(StochasticTransitionFeature.class);
        //okTrigger.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(triggerValue), MarkingExpr.from("1", net)));
        okTrigger.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(triggerValue + "*Ok", net)));

        Transition errTrigger = net.getTransition("rejErrTrigger");
        errTrigger.removeFeature(StochasticTransitionFeature.class);
        errTrigger.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(triggerValue + "*Err", net)));
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
    public String getNumberedUnreliabilityReward(int module) {
        return "Ko>=" + module;
    }

    @Override
    public String getNickName() {
        return "uncoordinated";
    }
}

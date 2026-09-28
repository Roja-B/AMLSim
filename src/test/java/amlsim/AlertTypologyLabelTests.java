package amlsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import amlsim.model.aml.AMLTypology;

import java.util.Random;
import java.util.logging.Logger;

import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;


// Issue #1: bipartite and stack transactions must carry the alert's SAR flag and alert ID
class AlertTypologyLabelTests {
    private static final int BIPARTITE = 4;
    private static final int STACK = 5;
    private static final long ALERT_ID = 42L;

    public SimProperties simProperties;
    public Random random;

    @BeforeEach
    void beforeEach()
    {
        this.simProperties = mock(SimProperties.class);
        when(this.simProperties.getMinTransactionAmount()).thenReturn(10.0);
        when(this.simProperties.getMaxTransactionAmount()).thenReturn(100.0);
        when(this.simProperties.getMarginRatio()).thenReturn(0.1);
        this.random = new Random(1);
    }

    private void stubAMLSim(MockedStatic<AMLSim> mocked)
    {
        mocked.when(AMLSim::getRandom).thenReturn(new Random(1));
        mocked.when(AMLSim::getSimProp).thenReturn(this.simProperties);
        mocked.when(AMLSim::getLogger).thenReturn(Logger.getLogger("AMLSim"));
    }

    private Alert buildAlert(int modelID, Account... members)
    {
        AMLTypology model = AMLTypology.createTypology(modelID, 10, 100, 0, 10);
        Alert alert = new Alert(ALERT_ID, model, null);
        for (Account member : members) {
            alert.addMember(member);
        }
        alert.setMainAccount(members[0]);
        members[0].setSAR(true);
        return alert;
    }

    @Test
    void bipartiteTransactionsAreSARLabeled()
    {
        long step = 1;

        try (MockedStatic<AMLSim> mocked = mockStatic(AMLSim.class)) {
            stubAMLSim(mocked);

            Account orig = new Account("1", 1, 1000.0f, "bankid", this.random);
            Account bene = new Account("2", 1, 1000.0f, "bankid", this.random);
            Alert alert = buildAlert(BIPARTITE, orig, bene);

            alert.registerTransactions(step, orig);

            mocked.verify(() -> AMLSim.handleTransaction(eq(step), any(), anyDouble(), eq(orig), eq(bene), eq(true), eq(ALERT_ID)), times(1));
        }
    }

    @Test
    void stackTransactionsAreSARLabeled()
    {
        long step = 1;

        try (MockedStatic<AMLSim> mocked = mockStatic(AMLSim.class)) {
            stubAMLSim(mocked);

            Account orig = new Account("1", 1, 1000.0f, "bankid", this.random);
            Account mid = new Account("2", 1, 1000.0f, "bankid", this.random);
            Account bene = new Account("3", 1, 1000.0f, "bankid", this.random);
            Alert alert = buildAlert(STACK, orig, mid, bene);

            alert.registerTransactions(step, orig);

            mocked.verify(() -> AMLSim.handleTransaction(eq(step), any(), anyDouble(), eq(orig), eq(mid), eq(true), eq(ALERT_ID)), times(1));
        }
    }
}

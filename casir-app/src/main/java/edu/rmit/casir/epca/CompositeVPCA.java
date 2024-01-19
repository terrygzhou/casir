package edu.rmit.casir.epca;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import api.PFSPCompiler;
import edu.rmit.casir.lpca.CompositeLPCA;
import edu.rmit.casir.lpca.LabeledPCA;
import edu.rmit.casir.util.GeneralUtil;
import lts.ActionName;
import lts.CompactState;
import lts.EventState;
import lts.LTSException;
import lts.Pair;
import lts.Transition;

/**
 * A labeled PCA as a result of composing a set of ePCA via unfolded PCA A
 * composed EPCA synchronized from a set of EPCA
 * 
 * @author terryzhou
 *
 */
public class CompositeVPCA {

	static Logger logger = Logger.getLogger(CompositeVPCA.class);

	String compositeName;

	// a set of ePCA
	Vector<VPCA> vpcaVector;

	// * local labeled PCA
	Vector<LabeledPCA> localUnfoldLpcaVec = new Vector<LabeledPCA>();

	// The global behaviour of CompositeEPCA is described in CompositeLPCA
	CompositeLPCA composedLPCA;

	// ****************************** Methods *************************************
	/**
	 * Passing the minimal input i.e. EPCA objects and composite processID.
	 * <p>
	 * for each EPCA, unfold it to a PCA and make it for composition
	 * 
	 */
	public CompositeVPCA(String compositeProcessName, Vector<VPCA> vpcas) {
		this.compositeName = compositeProcessName;
		String compositeNameStr = "";
		this.vpcaVector = vpcas;
		// this.unfoldedPCAs = new Vector<CompactStateRef>(epcas.size());

		// * Compose to global PCA
		Vector<CompactState> machines = new Vector<CompactState>(vpcas.size());

		PFSPCompiler comp = new PFSPCompiler();
		for (int i = 0; i < vpcas.size(); i++) {
			VPCA vpca = this.vpcaVector.get(i);
			compositeNameStr = compositeNameStr + vpca.getProcessName();

			if (i < vpcas.size() - 1)
				compositeNameStr = compositeNameStr + "_";

			LabeledPCA lpca = vpca.getUnfoldedLabeledPCA();
			logger.debug(lpca.getVarLabel());
			logger.debug(lpca.getLTS());
			CompactState unfoldPCA = lpca.getPcaObj();
			machines.add(unfoldPCA);

			// CompactStateRef localUnfoldPCA = new CompactStateRef(unfoldPCA);
			// this.unfoldedPCAs.add(i, localUnfoldPCA);

			// * build labeledUnfoldedPCA
			this.localUnfoldLpcaVec.add(vpca.getUnfoldedLabeledPCA());
		}

		this.compositeName = compositeNameStr;

		// composition happens here
		CompactState composite = comp.compose(this.compositeName, machines);

		// * Construct the global composed PCA, without state labels
		// this.compositePcaRef = new CompactStateRef(composite);
		CompactState cs = CompactStateRef.getRoundProbPCA(composite, 4);

		// build compositeLPCA
		this.composedLPCA = new CompositeLPCA(this.localUnfoldLpcaVec);
	}

	/**
	 * Gets the target state.
	 *
	 * @param source
	 *            the source
	 * @param label
	 *            the label
	 * @param pca
	 *            the pca
	 * @return the tgt state
	 */
	private int getTgtState(int source, String label, CompactState pca) {
		EventState current = pca.states[source];
		while (current != null) {
			ActionName act = current.getEventPCA();
			if (label.equals(act.getLabel()))
				return current.getNext();
			current = current.getList();
		}
		return -2;
	}


	/**
	 * Gets the composite pca ref.
	 *
	 * @return the composite pca ref
	 */
	public CompactStateRef getComposedPCA() {
		return composedLPCA.getGlobalPCA().getComposite();
	}

	public Vector<LabeledPCA> getLocalUnfoldLpcaVec() {
		return localUnfoldLpcaVec;
	}

	public void setLocalUnfoldLpcaVec(Vector<LabeledPCA> localUnfoldLpcaVec) {
		this.localUnfoldLpcaVec = localUnfoldLpcaVec;
	}

	public Vector<CompactStateRef> getUnfoldedPCAs() {
		Vector<CompactStateRef> pcaVec = new Vector<CompactStateRef>();
		for (LabeledPCA p : this.getLocalUnfoldLpcaVec()) {
			CompactStateRef pca = new CompactStateRef(p.getPcaObj());
			pcaVec.add(pca);
		}
		return pcaVec;
	}

	public CompositeLPCA getComposedLPCA() {
		return composedLPCA;
	}

	public void setComposedLPCA(CompositeLPCA composedLPCA) {
		this.composedLPCA = composedLPCA;
	}

	public String getCompositeName() {
		return compositeName;
	}

}

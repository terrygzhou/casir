package edu.rmit.casir.pca;

import org.apache.log4j.Logger;

import api.PFSPCompiler;
import edu.rmit.casir.util.GeneralUtil;
import lts.ActionName;
import lts.CompactState;
import lts.EventState;
import lts.SymbolTable;

public class PCAUtil {
	public static Logger logger = Logger.getLogger(PCAUtil.class);

	/**
	 * Given a PCA, and normalise it to merged probability for same transitions
	 * 
	 * @param cs
	 * @return
	 */
	public static CompactState normalisePCA(CompactState pca) {
		StringBuffer sb = new StringBuffer();
		sb.append("pca \n");
		// sb.append(pca.name + " = Q0,\n");
		sb.append(pca.name + " = Q0,");

		for (int i = 0; i < pca.maxStates; ++i) {
			// sb.append("Q" + i + "\t= ");
			sb.append("Q" + i + " = ");

			EventState current = pca.states[i];

			if (current == null) {
				if (i == pca.getEndseq())
					// sb.append("END \n");
					sb.append("END");

				else
					// sb.append("STOP \n");
					sb.append("STOP");

				if (i < pca.maxStates - 1)
					// sb.append(",\n");
					sb.append(",");

				else
					sb.append(".\n");
			} else {
				sb.append("(");
				int targetStateID = -3;
				String tranLabel = null;
				double prevProb = 0;
				int x = 0, y = 0;
				while (current != null) {

					ActionName label = current.getEventPCA();

					/**
					 * check if the targetState has been visited
					 */
					if (current.getNext() == targetStateID && label.getLabel().equals(tranLabel)) {
						logger.debug("need normalise here");
						// prevProb = prevProb + label.getProbability();
						prevProb = prevProb + GeneralUtil.round(label.getProbability(), 4);
						logger.debug(sb.toString());
//						sb.delete(x - 2, y);
						sb.delete(x - 1, y);
						logger.debug(sb.toString());
						sb.append(label.getTypeString() + "<" + prevProb + "> " + label.getLabel()
								+ " -> ");

					} else {
						x = sb.length();
						// prevProb = label.getProbability();
						prevProb = GeneralUtil.round(label.getProbability(), 4);

						sb.append(label.getTypeString() + "<" + prevProb + "> " + label.getLabel()
								+ " -> ");
						tranLabel = label.getLabel();

					}
					if (current.getNext() < 0)
						sb.append("ERROR");
					else
						sb.append("Q" + current.getNext());

					y = sb.length();

					targetStateID = current.getNext();

					current = current.getList();

					if (current == null){
						if (i < pca.maxStates - 1)
							// sb.append("),\n");
							sb.append("),");

						else
							// sb.append(").\n");
							sb.append(").");
					}
					else {
						// sb.append("\n\t |");
						sb.append(" |");
					}
				} // while
			}
		}
		logger.info(sb.toString());
		SymbolTable.init();
		PFSPCompiler comp = new PFSPCompiler();
		CompactState normalPca = comp.compile(pca.name, sb.toString());

		return normalPca;
	}

}

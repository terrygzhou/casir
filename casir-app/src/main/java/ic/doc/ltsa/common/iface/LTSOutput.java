package ic.doc.ltsa.common.iface;

/**
 * This interface is a patch for the missing package that should be provided by LTSA-MSC
 * 
 * @author terryzhou
 * 
 */
public abstract interface LTSOutput {
	public abstract void out(String paramString);

	public abstract void outln(String paramString);

	public abstract void clearOutput();
}

// public class LTSOutput implements lts.LTSOutput
// {
// public void out(String paramString){
// System.out.print(paramString);
// }
//
// public void outln(String paramString){
// System.out.println(paramString);
// }
//
// public void clearOutput(){
// System.out.println("CLEAR ALL");
// }
// }

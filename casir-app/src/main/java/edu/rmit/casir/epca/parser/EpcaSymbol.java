package edu.rmit.casir.epca.parser;

public class EpcaSymbol {

	public int kind;
	public int startPos;
	public int endPos;
	public String string;
	public int longValue;
	double doubleValue;
	public Object any;
	public static final int CONSTANT = 1;
	public static final int PROPERTY = 2;
	public static final int RANGE = 3;
	public static final int IF = 4;
	public static final int THEN = 5;
	public static final int ELSE = 6;
	public static final int FORALL = 7;
	public static final int WHEN = 8;
	public static final int SET = 9;
	public static final int PROGRESS = 10;
	public static final int MENU = 11;
	public static final int ANIMATION = 12;
	public static final int ACTIONS = 13;
	public static final int CONTROLS = 14;
	public static final int DETERMINISTIC = 15;
	public static final int MINIMAL = 16;
	public static final int COMPOSE = 17;
	public static final int TARGET = 18;
	public static final int IMPORT = 19;
	public static final int UNTIL = 20;
	public static final int ASSERT = 21;
	public static final int PREDICATE = 22;
	public static final int NEXTTIME = 23;
	public static final int EXISTS = 24;
	public static final int RIGID = 25;
	public static final int CONSTRAINT = 26;
	public static final int LTLPROPERTY = 27;
	public static final int SAFE = 28;
	public static final int INIT = 29;
	public static final int BOOLEAN_TYPE = 102;
	public static final int DOUBLE_TYPE = 103;
	public static final int INT_TYPE = 104;
	public static final int STRING_TYPE = 105;
	public static final int UNKNOWN_TYPE = 106;
	public static final int UPPERIDENT = 123;
	public static final int IDENTIFIER = 124;
	public static final int PCA = 201;
	public static final int UNARY_MINUS = 33;
	public static final int UNARY_PLUS = 34;
	public static final int PLUS = 35;
	public static final int MINUS = 36;
	public static final int STAR = 37;
	public static final int DIVIDE = 38;
	public static final int MODULUS = 39;
	public static final int CIRCUMFLEX = 40;
	public static final int SINE = 41;
	public static final int QUESTION = 42;
	public static final int COLON = 43;
	public static final int COMMA = 44;
	public static final int OR = 45;
	public static final int BITWISE_OR = 46;
	public static final int AND = 47;
	public static final int BITWISE_AND = 48;
	public static final int NOT_EQUAL = 49;
	public static final int PLING = 50;
	public static final int LESS_THAN_EQUAL = 51;
	public static final int LESS_THAN = 52;
	public static final int SHIFT_LEFT = 53;
	public static final int GREATER_THAN_EQUAL = 54;
	public static final int GREATER_THAN = 55;
	public static final int SHIFT_RIGHT = 56;
	public static final int EQUALS = 57;
	public static final int LROUND = 58;
	public static final int RROUND = 59;
	public static final int LCURLY = 60;
	public static final int RCURLY = 61;
	public static final int LSQUARE = 62;
	public static final int RSQUARE = 63;
	public static final int BECOMES = 64;
	public static final int SEMICOLON = 65;
	public static final int DOT = 66;
	public static final int DOT_DOT = 67;
	public static final int AT = 68;
	public static final int ARROW = 69;
	public static final int BACKSLASH = 70;
	public static final int COLON_COLON = 71;
	public static final int QUOTE = 72;
	public static final int HASH = 73;
	public static final int EVENTUALLY = 74;
	public static final int ALWAYS = 75;
	public static final int EQUIVALENT = 76;
	public static final int WEAKUNTIL = 77;
	public static final int LABELCONST = 98;
	public static final int EOFSYM = 99;
	public static final int COMMENT = 100;
	public static final int INTERFACE = 101;
	public static final int IO = 200;
	public static final int UNUSED_BEHAVIOUR = 202;
	public static final int INT_VALUE = 125;
	public static final int DOUBLE_VALUE = 126;
	public static final int STRING_VALUE = 127;
	// --------add EPCA kind -----------------
	public static final int EPCA = 301;
	public static final int VARIABLE = 302;
	public static final int PROBABILITY = 303;
	public static final int VAR_VALUE = 304;
	public static final int BOOL_VALUE = 305;
	public static final int ELSE_IF = 306;

	public EpcaSymbol() {
		this.endPos = -1;
		this.kind = 106;
	}

	public EpcaSymbol(EpcaSymbol copy) {
		this.endPos = -1;
		this.kind = copy.kind;
		this.startPos = copy.startPos;
		this.endPos = copy.endPos;
		this.string = copy.string;
		this.longValue = copy.longValue;
		this.any = copy.any;
	}

	public EpcaSymbol(EpcaSymbol copy, String name) {
		this(copy);
		this.string = name;
	}

	public EpcaSymbol(int kind) {
		this.endPos = -1;
		this.kind = kind;
		this.startPos = -1;
		this.string = null;
		this.longValue = 0;
		this.doubleValue = 0.0D;
	}

	public EpcaSymbol(int kind, String s) {
		this.endPos = -1;

		this.kind = kind;
		this.startPos = -1;
		this.string = s;
		this.longValue = 0;
		this.doubleValue = 0.0D;
	}

	public EpcaSymbol(int kind, int v) {
		this.endPos = -1;
		this.kind = kind;
		this.startPos = -1;
		this.string = null;
		this.longValue = v;
		this.doubleValue = 0.0D;
	}

	public EpcaSymbol(int kind, double v) {
		this.endPos = -1;

		this.kind = kind;
		this.startPos = -1;
		this.string = null;
		this.longValue = 0;
		this.doubleValue = v;
	}

	public void setString(String s) {
		this.string = s;
	}

	public void setValue(int val) {
		this.longValue = val;
	}

	public int intValue() {
		return this.longValue;
	}

	public void setValue(double val) {
		this.doubleValue = val;
	}

	public double doubleValue() {
		return this.doubleValue;
	}

	public void setAny(Object o) {
		this.any = o;
	}

	public Object getAny() {
		return this.any;
	}

	public boolean isScalarType() {
		switch (this.kind) {
		case 102:
		case 103:
		case 104:
		case 105:
			return true;
		}

		return false;
	}

	public String toString() {
		switch (this.kind) {
		case 1:
			return "const";
		case 2:
			return "property";
		case 3:
			return "range";
		case 4:
			return "if";
		case 5:
			return "then";
		case 6:
			return "else";
		case 7:
			return "forall";
		case 8:
			return "when";
		case 9:
			return "set";
		case 10:
			return "progress";
		case 11:
			return "menu";
		case 12:
			return "animation";
		case 13:
			return "actions";
		case 14:
			return "controls";
		case 15:
			return "determinstic";
		case 16:
			return "minimal";
		case 17:
			return "compose";
		case 18:
			return "target";
		case 19:
			return "import";
		case 20:
			return "U";
		case 21:
			return "assert";
		case 22:
			return "fluent";
		case 23:
			return "X";
		case 24:
			return "exists";
		case 25:
			return "rigid";
		case 26:
			return "constraint";
		case 27:
			return "ltl_property";
		case 28:
			return "safe";
		case 29:
			return "initially";
		case 102:
			return "boolean";
		case 103:
			return "double";
		case 104:
			return "int";
		case 105:
			return "string";
		case 106:
			return "unknown";
		case 201:
			return "pca";
		case 123: // processID
		case 124:
		case 98:
		case 127:
			return this.string;
		case 125:
			return this.longValue + "";
		case 126:
			return this.doubleValue + "";
		case 33:
			return "-";
		case 34:
			return "+";
		case 35:
			return "+";
		case 36:
			return "-";
		case 37:
			return "*";
		case 38:
			return "/";
		case 39:
			return "%";
		case 40:
			return "^";
		case 41:
			return "~";
		case 42:
			return "?";
		case 43:
			return ":";
		case 71:
			return "::";
		case 44:
			return ",";
		case 45:
			return "||";
		case 46:
			return "|";
		case 47:
			return "&&";
		case 48:
			return "&";
		case 49:
			return "!=";
		case 50:
			return "!";
		case 51:
			return "<=";
		case 52:
			return "<";
		case 53:
			return "<<";
		case 54:
			return ">=";
		case 55:
			return ">";
		case 56:
			return ">>";
		case 57:
			return "==";
		case 58:
			return "(";
		case 59:
			return ")";
		case 72:
			return "'";
		case 73:
			return "#";
		case 74:
			return "<>";
		case 75:
			return "[]";
		case 76:
			return "<->";
		case 60:
			return "{";
		case 61:
			return "}";
		case 62:
			return "[";
		case 63:
			return "]";
		case 64:
			return "=";
		case 65:
			return ";";
		case 66:
			return ".";
		case 67:
			return "..";
		case 68:
			return "@";
		case 101:
			return "@-";
		case 202:
			return "@*";
		case 200:
			return "IO";
		case 69:
			return "->";
		case 70:
			return "\\";
		case 99:
			return "EOF";

			// -------added for EPCA------------------------
		case 301:
			return "epca";
		case 302:
			return "VAR";
		case 303:
			return this.doubleValue + "";
		case 305:// boolean value
			return this.string;
		case 306:
			return "elseif";
			// -------end of the EPCA extension-------------
		case 30:
		case 31:
		case 32:
		case 77:
		case 78:
		case 79:
		case 80:
		case 81:
		case 82:
		case 83:
		case 84:
		case 85:
		case 86:
		case 87:
		case 88:
		case 89:
		case 90:
		case 91:
		case 92:
		case 93:
		case 94:
		case 95:
		case 96:
		case 97:
		case 100:
		case 107:
		case 108:
		case 109:
		case 110:
		case 111:
		case 112:
		case 113:
		case 114:
		case 115:
		case 116:
		case 117:
		case 118:
		case 119:
		case 120:
		case 121:
		case 122:
		case 128:
		case 129:
		case 130:
		case 131:
		case 132:
		case 133:
		case 134:
		case 135:
		case 136:
		case 137:
		case 138:
		case 139:
		case 140:
		case 141:
		case 142:
		case 143:
		case 144:
		case 145:
		case 146:
		case 147:
		case 148:
		case 149:
		case 150:
		case 151:
		case 152:
		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
		case 168:
		case 169:
		case 170:
		case 171:
		case 172:
		case 173:
		case 174:
		case 175:
		case 176:
		case 177:
		case 178:
		case 179:
		case 180:
		case 181:
		case 182:
		case 183:
		case 184:
		case 185:
		case 186:
		case 187:
		case 188:
		case 189:
		case 190:
		case 191:
		case 192:
		case 193:
		case 194:
		case 195:
		case 196:
		case 197:
		case 198:
		case 199:
		}
		return "ERROR";
	}
}
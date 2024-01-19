package edu.rmit.casir.epca.parser;

import lts.Diagnostics;
import lts.LTSInput;

public class EpcaLex {

	private LTSInput input;
	private EpcaSymbol symbol;
	private char ch;
	private boolean eoln;
	private boolean newSymbols;
	private boolean isProbability;
	private EpcaSymbol current;
	private EpcaSymbol buffer;

	public EpcaLex(LTSInput input) {
		this(input, true);
	}

	public EpcaLex(LTSInput input, boolean newSymbols) {
		this.newSymbols = true;

		this.current = null;
		this.buffer = null;

		this.input = input;
		this.newSymbols = newSymbols;
		if (!(newSymbols))
			this.symbol = new EpcaSymbol();
	}

	private void error(String errorMsg) {
		Diagnostics.fatal(errorMsg, new Integer(this.input.getMarker()));
	}

	private void next_ch() {
		this.ch = this.input.nextChar();
		this.eoln = ((this.ch == '\n') || (this.ch == 0));
	}

	private void back_ch() {
		this.ch = this.input.backChar();
		this.eoln = ((this.ch == '\n') || (this.ch == 0));
	}

	private void in_comment() {
		if (this.ch == '/') {
			do
				next_ch();
			while (!(this.eoln));
		} else {
			do {
				do
					next_ch();
				while ((this.ch != '*') && (this.ch != 0));
				do
					next_ch();
				while ((this.ch == '*') && (this.ch != 0));
			} while ((this.ch != '/') && (this.ch != 0));
			next_ch();
		}
		if (!(this.newSymbols)) {
			this.symbol.kind = 100;
			back_ch();
		}
	}

	private boolean isodigit(char ch) {
		return ((ch >= '0') && (ch <= '7'));
	}

	private boolean isxdigit(char ch) {
		return (((ch >= '0') && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F')) || ((ch >= 'a') && (ch <= 'f')));
	}

	private boolean isbase(char ch, int base) {
		switch (base) {
		case 10:
			return Character.isDigit(ch);
		case 16:
			return isxdigit(ch);
		case 8:
			return isodigit(ch);
		}
		return true;
	}

	public void probabilitySpecification() {
		this.isProbability = true;
	}

	private void in_number() {
		long intValue = 0L;
		int digit = 0;
		int base = 10;
		boolean numIsDouble = false;
		this.symbol.kind = 125;
		next_ch();
		if (this.ch == '.') {
			back_ch();
			back_ch();
			if (this.ch == '<')
				this.probabilitySpecification();
			next_ch();
			next_ch();
		}
		back_ch();
		if (this.ch == '0') {
			next_ch();
			if ((this.ch == 'x') || (this.ch == 'X')) {
				base = 16;
				next_ch();
			} else if (this.isProbability) {
				base = 10;
			} else {
				base = 8;
			}
		} else {
			base = 10;
		}

		StringBuffer realBuf = new StringBuffer();

		while (isbase(this.ch, base)) {
			realBuf.append(this.ch);
			switch (base) {
			case 8:
			case 10:
				digit = this.ch - '0';
				break;
			case 16:
				if (Character.isUpperCase(this.ch))
					digit = this.ch - 'A' + 10;
				else if (Character.isLowerCase(this.ch))
					digit = this.ch - 'a' + 10;
				else {
					digit = this.ch - '0';
				}
			}
			if (intValue * base > 2147483647 - digit) {
				error("Integer Overflow");
				intValue = 2147483647L;
				break;
			}
			intValue = intValue * base + digit;
			next_ch();
		}

		if ((this.isProbability) && (base == 10)) {
			if (this.ch == '.') {
				numIsDouble = true;
				do {
					realBuf.append(this.ch);
					next_ch();
				} while (Character.isDigit(this.ch));
			}

			if ((this.ch == 'e') || (this.ch == 'E')) {
				numIsDouble = true;
				realBuf.append(this.ch);
				next_ch();

				if ((this.ch == '+') || (this.ch == '-')) {
					realBuf.append(this.ch);
					next_ch();
				}

				if (Character.isDigit(this.ch))
					while (Character.isDigit(this.ch)) {
						realBuf.append(this.ch);
						next_ch();
					}
				else {
					error("exponent expected after e or E");
				}
			}
			if (numIsDouble) {
				try {
					this.symbol.doubleValue = Double.valueOf(realBuf.toString()).doubleValue();
					this.symbol.kind = 126;
					if (this.isProbability)
						this.symbol.kind = EpcaSymbol.PROBABILITY;
				} catch (NumberFormatException msg) {
					error("Bad double value. " + msg);
				}
			}
		} else if ((this.ch == 'U') || (this.ch == 'u') || (this.ch == 'L') || (this.ch == 'U')) {
			next_ch();
		}
		this.symbol.setValue((int) intValue);
		back_ch();
		this.isProbability = false;
	}

	private void in_escseq() {
		while (this.ch == '\\') {
			next_ch();
			switch (this.ch) {
			case 'a':
				this.ch = 'a';
				break;
			case 'b':
				this.ch = '\b';
				break;
			case 'f':
				this.ch = '\f';
				break;
			case 'n':
				this.ch = '\n';
				break;
			case 'r':
				this.ch = '\r';
				break;
			case 't':
				this.ch = '\t';
				break;
			case 'v':
				break;
			case '\\':
				this.ch = '\\';
				break;
			case '\'':
				this.ch = '\'';
				break;
			case '"':
				this.ch = '"';
				break;
			case '?':
				this.ch = '?';
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				int n = this.ch - '0';
				next_ch();
				if (isodigit(this.ch)) {
					n = n * 8 + this.ch - 48;
					next_ch();
					if (isodigit(this.ch)) {
						n = n * 8 + this.ch - 48;
					}
				}
				this.ch = (char) n;
				break;
			case 'X':
			case 'x':
				// redeclare n here
				n = 0;
				next_ch();
				if (!(isxdigit(this.ch))) {
					error("hex digit expected after \\x");
				} else {
					int hex_digits = 0;
					while ((isxdigit(this.ch)) && (hex_digits < 2)) {
						++hex_digits;
						if (Character.isDigit(this.ch))
							n = n * 16 + this.ch - 48;
						else if (Character.isUpperCase(this.ch))
							n = n * 16 + this.ch - 65;
						else
							n = n * 16 + this.ch - 97;
						next_ch();
					}
				}
				this.ch = (char) n;
			}
		}
	}

	private void in_string() {
		char quote = this.ch;

		StringBuffer buf = new StringBuffer();
		int more;
		do {
			next_ch();

			if ((more = ((this.ch != quote) && (!(this.eoln))) ? 1 : 0) != 0)
				buf.append(this.ch);
		} while (more == 1);
		this.symbol.setString(buf.toString());
		if (this.eoln)
			error("No closing character for string constant");
		this.symbol.kind = 127;
	}

	private void in_identifier() {
		StringBuffer buf = new StringBuffer();
		do {
			buf.append(this.ch);
			next_ch();
			// add [, ], :: for EPCA
		} while ((Character.isLetterOrDigit(this.ch)) || (this.ch == '_')); // || (this.ch == ']')
//				|| (this.ch == '[') || (this.ch == ':')); || (this.ch == '+') || (this.ch == '-')
				//|| (this.ch == '*') || (this.ch == '/'));

		String s = buf.toString();
		if (s.equals("true") || s.equals("false")) {
			this.symbol.kind = EpcaSymbol.BOOL_VALUE;
			this.symbol.setString(s);
			back_ch();
			return;
		}

		this.symbol.setString(s);
		Object kind = EpcaSymbolTable.get(s);
		if (kind == null)
			if (Character.isUpperCase(s.charAt(0)))
				this.symbol.kind = 123;
			else
				this.symbol.kind = 124;
		else {
			this.symbol.kind = ((Integer) kind).intValue();
		}

		back_ch();
	}

	public EpcaSymbol in_sym() {
		next_ch();
		if (this.newSymbols) {
			this.symbol = new EpcaSymbol();
		}
		boolean DoOnce = true;

		while (DoOnce) {
			DoOnce = false;

			this.symbol.startPos = this.input.getMarker();
			switch (this.ch) {
			case '\0':
				this.symbol.kind = 99;
				break;
			case '\t':
			case '\n':
			case '\f':
			case '\r':
			case ' ':
				while (Character.isWhitespace(this.ch))
					next_ch();
				DoOnce = true;
				break;
			case '/':
				next_ch();
				if ((this.ch == '/') || (this.ch == '*')) {
					in_comment();
					if (this.newSymbols)
						DoOnce = true;
				} else {
					this.symbol.kind = 38;
					back_ch();
				}
				break;
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
			case '_':
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
			case 'o':
			case 'p':
			case 'q':
			case 'r':
			case 's':
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
				in_identifier();
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				in_number();
				break;
			case '#':
				this.symbol.kind = 73;
				break;
			case '\'':
				this.symbol.kind = 72;
				break;
			case '"':
				in_string();
				break;
			case '+':
				this.symbol.kind = 35;
				break;
			case '*':
				this.symbol.kind = 37;
				break;
			case '%':
				this.symbol.kind = 39;
				break;
			case '^':
				this.symbol.kind = 40;
				break;
			case '~':
				this.symbol.kind = 41;
				break;
			case '?':
				this.symbol.kind = 42;
				break;
			case ',':
				this.symbol.kind = 44;
				break;
			case '(':
				this.symbol.kind = 58;
				break;
			case ')':
				this.symbol.kind = 59;
				break;
			case '{':
				this.symbol.kind = 60;
				break;
			case '}':
				this.symbol.kind = 61;
				break;
			case ']':
				this.symbol.kind = 63;
				break;
			case ';':
				this.symbol.kind = 65;
				break;
			case '@':
				next_ch();
				if (this.ch == '-') {
					this.symbol.kind = 101;
				} else if (this.ch == '*') {
					this.symbol.kind = 202;
				} else {
					this.symbol.kind = 68;
					back_ch();
				}
				break;
			case '\\':
				this.symbol.kind = 70;
				break;
			case '[':
				next_ch();
				if (this.ch == ']') {
					this.symbol.kind = 75;
				} else {
					this.symbol.kind = 62;
					back_ch();
				}
				break;
			case '|':
				next_ch();
				if (this.ch == '|') {// if '||'
					this.symbol.kind = 45;
				} else {
					this.symbol.kind = 46;
					back_ch();
				}
				break;
			case '&':
				next_ch();
				if (this.ch == '&') {
					this.symbol.kind = 47;
				} else {
					this.symbol.kind = 48;
					back_ch();
				}
				break;
			case '!':
				next_ch();
				if (this.ch == '=') {
					this.symbol.kind = 49;
				} else {
					this.symbol.kind = 50;
					back_ch();
				}
				break;
			case '<':
				next_ch();
				if (this.ch == '=') {
					this.symbol.kind = 51;
				} else if (this.ch == '<') {
					this.symbol.kind = 53;
				} else if (this.ch == '>') {
					this.symbol.kind = 74;
				} else if (this.ch == '-') {
					next_ch();
					if (this.ch == '>') {
						this.symbol.kind = 76;
					} else {
						this.symbol.kind = 52;
						back_ch();
						back_ch();
					}
				}
				// else if (this.ch == '0') { // <0.x
				// next_ch();
				// if (this.ch == '.'){
				// this.symbol.kind = EpcaSymbol.PROBABILITY;
				// }
				// back_ch();
				// back_ch();
				// }
				else {
					this.symbol.kind = 52;
					back_ch();
				}
				break;
			case '>':
				next_ch();
				if (this.ch == '=') {
					this.symbol.kind = 54;
				} else if (this.ch == '>') {
					this.symbol.kind = 56;
				} else {
					this.symbol.kind = 55;
					back_ch();
				}
				break;
			case '=':
				next_ch();
				if (this.ch == '=') {
					this.symbol.kind = 57;
				} else {
					this.symbol.kind = 64;
					back_ch();
				}
				break;
			case '.':
				next_ch();
				if (this.ch == '.') {
					this.symbol.kind = 67;
				} else {
					this.symbol.kind = 66;
					back_ch();
				}
				break;
			case '-':
				next_ch();
				if (this.ch == '>') {
					this.symbol.kind = 69;
				} else {
					this.symbol.kind = 36;
					back_ch();
				}
				break;
			case ':':
				next_ch();
				if (this.ch == ':') {
					this.symbol.kind = 71;
				} else {
					this.symbol.kind = 43;
					back_ch();
				}
				break;
			case '\1':
			case '\2':
			case '\3':
			case '\4':
			case '\5':
			case '\6':
			case '\7':
			case '\b':
				// case '\11':
				// case '\14':
				// case '\15':
			case '\16':
			case '\17':
				// case '\18':
				// case '\19':
			case '\20':
			case '\21':
			case '\22':
			case '\23':
			case '\24':
			case '\25':
			case '\26':
			case '\27':
				// case '\28':
				// case '\29':
			case '\30':
			case '\31':
			case '$':
			case '`':
			default:
				error("unexpected character encountered");
			}
		}// while
		this.symbol.endPos = this.input.getMarker();
		return this.symbol;
	}

	public EpcaSymbol next_symbol() {
		if (this.buffer == null) {
			this.current = in_sym();
		} else {
			this.current = this.buffer;
			this.buffer = null;
		}
		return this.current;
	}

	public void push_symbol() {
		this.buffer = this.current;
	}

	public EpcaSymbol current() {
		return this.current;
	}

}

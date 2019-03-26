/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.util.demangler;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ghidra.program.database.data.DataTypeUtilities;
import ghidra.program.model.data.*;
import ghidra.program.model.data.Enum;
import util.demangler.*;

/**
 * A class to represent a demangled data type.
 */
public class DemangledDataType extends DemangledType {

	private static final Pattern ARRAY_SUBSCRIPT_PATTERN = Pattern.compile("\\[\\d*\\]");

	public static final char SPACE = ' ';

	public static final String UNSIGNED = "unsigned";
	public static final String SIGNED = "signed";

	public static final String ARR_NOTATION = "[]";
	public static final String REF_NOTATION = "&";
	public static final String PTR_NOTATION = "*";

	public static final String VOLATILE = "volatile";
	public static final String COMPLEX = "complex";
	public static final String CLASS = "class";
	public static final String ENUM = "enum";
	public static final String STRUCT = "struct";
	public static final String UNION = "union";
	public static final String CONST = "const";
	public static final String COCLASS = "coclass";
	public static final String COINTERFACE = "cointerface";

	public final static String VARARGS = "...";
	public final static String VOID = "void";
	public final static String BOOL = "bool";
	public final static String CHAR = "char";
	public final static String WCHAR_T = "wchar_t";
	public final static String SHORT = "short";
	public final static String INT = "int";
	public final static String INT0_T = "int0_t";//TODO
	public final static String LONG = "long";
	public final static String LONG_LONG = "long long";
	public final static String FLOAT = "float";
	public final static String DOUBLE = "double";
	public final static String INT8 = "__int8";
	public final static String INT16 = "__int16";
	public final static String INT32 = "__int32";
	public final static String INT64 = "__int64";
	public final static String INT128 = "__int128";//TODO
	public final static String FLOAT128 = "__float128";//TODO
	public final static String LONG_DOUBLE = "long double";
	public final static String PTR64 = "__ptr64";
	public final static String STRING = "string";
	public final static String UNDEFINED = "undefined";
	public static final String UNALIGNED = "__unaligned";
	public static final String RESTRICT = "__restrict";

	public final static String[] PRIMITIVES = { VOID, BOOL, CHAR, WCHAR_T, SHORT, INT, INT0_T, LONG,
		LONG_LONG, FLOAT, DOUBLE, INT128, FLOAT128, LONG_DOUBLE, };

	private int arrayDimensions = 0;
	private boolean isClass;
	private boolean isComplex;
	private boolean isEnum;
	private boolean isPointer64;
	private boolean isReference;
	private boolean isSigned;//explicitly signed!
	private boolean isStruct;
	private boolean isTemplate;
	private boolean isUnaligned;
	private boolean isUnion;
	private boolean isUnsigned;
	private boolean isVarArgs;
//	private boolean isVolatile;
	private int pointerLevels = 0;
	private String enumType;
	private boolean isRestrict;
	private String basedName;
	private String memberScope;
	private boolean isCoclass;
	private boolean isCointerface;

	/**
	 * Constructs a new demangled datatype.
	 * @param name the name of the datatype
	 */
	public DemangledDataType(String name) {
		super(name);
	}

	DemangledDataType(GenericDemangledDataType source) {
		super(source);

		if (source.isArray()) {
			// TODO GenericDemangledDataType should go away; if so, we don't need to worry
			//      about array dimension impedance
			arrayDimensions = 1;
		}

		isClass = source.isClass();
		isComplex = source.isComplex();
		isEnum = source.isEnum();
		isPointer64 = source.isPointer64();
		isReference = source.isReference();
		isSigned = source.isSigned();
		isStruct = source.isStruct();
		isTemplate = source.isTemplate();
		isUnaligned = source.isUnaligned();
		isUnion = source.isUnion();
		isUnsigned = source.isUnsigned();
		isVarArgs = source.isVarArgs();
//		isVolatile = source.isVolatile();
		pointerLevels = source.getPointerLevels();
		//enumType = source.getEnumType();
		isRestrict = source.isRestrict();
		basedName = source.getBasedName();
		memberScope = source.getMemberScope();
		isCoclass = source.isCoclass();
		isCointerface = source.isCointerface();

		GenericDemangledType otherNamespace = source.getNamespace();
		if (otherNamespace != null) {
			namespace = DemangledType.convertToNamespace(source.getNamespace());
		}

		GenericDemangledTemplate otherTemplate = source.getTemplate();
		if (otherTemplate != null) {
			template = new DemangledTemplate(otherTemplate);
		}

		if (source.isConst()) {
			setConst();
		}
	}

	public DemangledDataType copy() {
		DemangledDataType copy = new DemangledDataType(getName());
		copy(this, copy);
		return copy;
	}

	protected void copy(DemangledDataType source, DemangledDataType destination) {
		destination.arrayDimensions = source.arrayDimensions;
		destination.isClass = source.isClass;
		destination.isComplex = source.isComplex;
		destination.isEnum = source.isEnum;
		destination.isPointer64 = source.isPointer64;
		destination.isReference = source.isReference;
		destination.isSigned = source.isSigned;
		destination.isStruct = source.isStruct;
		destination.isTemplate = source.isTemplate;
		destination.isUnion = source.isUnion;
		destination.isUnsigned = source.isUnsigned;
		destination.isVarArgs = source.isVarArgs;
//		destination.isVolatile = source.isVolatile;
		destination.pointerLevels = source.pointerLevels;
		//destination.enumType = source.enumType;

		destination.isUnaligned = source.isUnaligned();
		destination.isRestrict = source.isRestrict();
		destination.basedName = source.getBasedName();
		destination.memberScope = source.getMemberScope();

		destination.setNamespace(source.getNamespace());
		destination.setTemplate(source.getTemplate());
		destination.isCoclass = source.isCoclass;
		destination.isCointerface = source.isCointerface;

		if (source.isConst()) {
			destination.setConst();
		}
	}

	/**
	 * Converts this demangled datatype into the corresponding Ghidra datatype.
	 * @param dataTypeManager the data type manager to be searched and whose data organization
	 * should be used
	 * @return the Ghidra datatype corresponding to the demangled datatype
	 */
	public DataType getDataType(DataTypeManager dataTypeManager) {
		String name = getName();
		if (name == null) {
			return DataType.DEFAULT;
		}

		DataType dt = null;

		if (namespace == null) {
			dt = getBuiltInType(dataTypeManager);
		}

		if (dt == null) {

			// If custom type, look for it first
			// TODO: this find method could be subject to name mismatch, although
			// presence of namespace could help this if it existing and contained within
			// an appropriate namespace category
			dt = findDataType(dataTypeManager, namespace, name);

			DataType baseType = dt;
			if (dt instanceof TypeDef) {
				baseType = ((TypeDef) dt).getBaseDataType();
			}

			if (isStruct()) {
				if (baseType == null || !(baseType instanceof Structure)) {
					// Fill it with nonsense
					dt = createPlaceHolderStructure(name, getNamespace());
				}
			}
			else if (isUnion()) {
				if (baseType == null || !(baseType instanceof Union)) {
					dt = new UnionDataType(getDemanglerCategoryPath(name, getNamespace()), name);
				}
			}
			else if (isEnum()) {
				if (baseType == null || !(baseType instanceof Enum)) {
					// TODO: Can't tell how big an enum is,
					//   Just use the size of a pointer
					// 20170522: Modified following code to allow "some" sizing from MSFT.
					if ((enumType == null) || "int".equals(enumType) ||
						"unsigned int".equals(enumType)) {
						dt = new EnumDataType(getDemanglerCategoryPath(name, getNamespace()), name,
							dataTypeManager.getDataOrganization().getIntegerSize());
					}
					else if ("char".equals(enumType) || "unsigned char".equals(enumType)) {
						dt = new EnumDataType(getDemanglerCategoryPath(name, getNamespace()), name,
							dataTypeManager.getDataOrganization().getCharSize());

					}
					else if ("short".equals(enumType) || "unsigned short".equals(enumType)) {
						dt = new EnumDataType(getDemanglerCategoryPath(name, getNamespace()), name,
							dataTypeManager.getDataOrganization().getShortSize());

					}
					else if ("long".equals(enumType) || "unsigned long".equals(enumType)) {
						dt = new EnumDataType(getDemanglerCategoryPath(name, getNamespace()), name,
							dataTypeManager.getDataOrganization().getLongSize());
					}
					else {
						dt = new EnumDataType(getDemanglerCategoryPath(name, getNamespace()), name,
							dataTypeManager.getDataOrganization().getIntegerSize());
					}
				}
			}
			else if (isClass() || name.equals(STRING)) {//TODO - class datatypes??
				if (baseType == null || !(baseType instanceof Structure)) {
					// try creating empty structures for unknown types instead.
					dt = createPlaceHolderStructure(name, getNamespace());
				}
			}
			else if (dt == null) { // TODO: Is using whatever was found OK ??

				// I don't know what this is
				// If it isn't pointed to, or isn't a referent, then assume typedef.
				if (!(isReference() || isPointer())) { // Unknown type
					dt = new TypedefDataType(getDemanglerCategoryPath(name, getNamespace()), name,
						new DWordDataType());
				}
				else {
					// try creating empty structures for unknown types instead.
					dt = createPlaceHolderStructure(name, getNamespace());
				}
			}

		}

		int numPointers = getPointerLevels();
		if (isReference()) {
			numPointers++;
		}

		for (int i = 0; i < numPointers; ++i) {
			dt = PointerDataType.getPointer(dt, dataTypeManager);
		}
		return dt;
	}

	private DataType getBuiltInType(DataTypeManager dataTypeManager) {
		DataType dt = null;
		String name = getName();
		if (BOOL.equals(name)) {
			dt = BooleanDataType.dataType;
		}
		else if (VOID.equals(name)) {
			dt = VoidDataType.dataType;
		}
		else if (CHAR.equals(name)) {
			if (isUnsigned()) {
				dt = UnsignedCharDataType.dataType;
			}
			else {
				dt = CharDataType.dataType;
			}
		}
		else if (SHORT.equals(name)) {
			if (isUnsigned()) {
				dt = UnsignedShortDataType.dataType;
			}
			else {
				dt = ShortDataType.dataType;
			}
		}
		else if (INT.equals(name)) {
			if (isUnsigned()) {
				dt = UnsignedIntegerDataType.dataType;
			}
			else {
				dt = IntegerDataType.dataType;
			}
		}
		else if (LONG.equals(name)) {
			if (isUnsigned()) {
				dt = UnsignedLongDataType.dataType;
			}
			else {
				dt = LongDataType.dataType;
			}
		}
		else if (LONG_LONG.equals(name)) {
			if (isUnsigned()) {
				dt = UnsignedLongLongDataType.dataType;
			}
			else {
				dt = LongLongDataType.dataType;
			}
		}
		else if (UNSIGNED.equals(name)) {
			dt = UnsignedIntegerDataType.dataType;
		}
		else if (FLOAT.equals(name)) {
			dt = FloatDataType.dataType;
		}
		else if (DOUBLE.equals(name)) {
			dt = DoubleDataType.dataType;
		}
		else if (LONG_DOUBLE.equals(name)) {
			dt = LongDoubleDataType.dataType;
		}
		else if (INT8.equals(name)) {
			if (isUnsigned()) {
				dt = new TypedefDataType("__uint8",
					AbstractIntegerDataType.getUnsignedDataType(1, dataTypeManager));
			}
			else {
				dt = new TypedefDataType(INT8,
					AbstractIntegerDataType.getSignedDataType(1, dataTypeManager));
			}
		}
		else if (INT16.equals(name)) {
			if (isUnsigned()) {
				dt = new TypedefDataType("__uint16",
					AbstractIntegerDataType.getUnsignedDataType(2, dataTypeManager));
			}
			else {
				dt = new TypedefDataType(INT16,
					AbstractIntegerDataType.getSignedDataType(2, dataTypeManager));
			}
		}
		else if (INT32.equals(name)) {
			if (isUnsigned()) {
				dt = new TypedefDataType("__uint32",
					AbstractIntegerDataType.getUnsignedDataType(4, dataTypeManager));
			}
			else {
				dt = new TypedefDataType(INT32,
					AbstractIntegerDataType.getSignedDataType(4, dataTypeManager));
			}
		}
		else if (INT64.equals(name)) {
			if (isUnsigned()) {
				dt = new TypedefDataType("__uint64",
					AbstractIntegerDataType.getUnsignedDataType(8, dataTypeManager));
			}
			else {
				dt = new TypedefDataType(INT64,
					AbstractIntegerDataType.getSignedDataType(8, dataTypeManager));
			}
		}
		else if (UNDEFINED.equals(name)) {
			dt = DataType.DEFAULT;
		}
		return dt;
	}

	/**
	 * Find non-builtin type
	 * @param dataTypeManager data type manager to be searched
	 * @param dtName name of data type
	 * @param namespace namespace associated with dtName or null if not applicable.  If specified, 
	 * a namespace-base category path will be given precendence.
	 * @return data type if found, otherwise null.
	 * @see DataTypeUtilities#findDataType(DataTypeManager, ghidra.program.model.symbol.Namespace, String, Class) for similar namespace
	 * based search.
	 */
	static DataType findDataType(DataTypeManager dataTypeManager, DemangledType namespace,
			String dtName) {
		// TODO: Should be able to search archives somehow
		ArrayList<DataType> list = new ArrayList<>();
		dataTypeManager.findDataTypes(dtName, list);
		if (!list.isEmpty()) {
			//use the datatype that exists in the root category,
			//otherwise just pick the first one...
			DataType anyDt = null;
			DataType preferredDataType = null;
			for (DataType existingDT : list) {
				if (existingDT instanceof BuiltIn) {
					continue; // TODO: not sure if this is good - built-ins handled explicitly 
					// by DemangledDataType.getDataType method
				}
				if (namespace == null) {
					if (existingDT.getCategoryPath().equals(CategoryPath.ROOT)) {
						return existingDT;
					}
					anyDt = existingDT;
				}
				if (isNamespaceCategoryMatch(existingDT, namespace)) {
					preferredDataType = existingDT;
				}
			}
			if (preferredDataType != null) {
				return preferredDataType;
			}
			return anyDt;
		}
		return null;
	}

	private static boolean isNamespaceCategoryMatch(DataType dt, DemangledType namespace) {
		if (namespace == null) {
			return true;
		}
		DemangledType ns = namespace;
		CategoryPath categoryPath = dt.getCategoryPath();
		while (ns != null) {
			if (categoryPath.equals(CategoryPath.ROOT) ||
				!categoryPath.getName().equals(ns.getName())) {
				return false;
			}
			categoryPath = categoryPath.getParent();
			ns = ns.getNamespace();
		}
		return true;
	}

	private static String getNamespacePath(String dtName, DemangledType namespace) {
		DemangledType ns = namespace;
		String namespacePath = "";
		while (ns != null) {
			namespacePath = "/" + ns.getName() + namespacePath;
			ns = ns.getNamespace();
		}
		return namespacePath;
	}

	private static CategoryPath getDemanglerCategoryPath(String dtName, DemangledType namespace) {
		return new CategoryPath("/Demangler" + getNamespacePath(dtName, namespace));
	}

	static Structure createPlaceHolderStructure(String dtName, DemangledType namespace) {
		StructureDataType structDT = new StructureDataType(dtName, 0);
		structDT.setDescription("PlaceHolder Structure");
		structDT.setCategoryPath(getDemanglerCategoryPath(dtName, namespace));

		return structDT;
	}

	public int getPointerLevels() {
		return pointerLevels;
	}

	public void incrementPointerLevels() {
		pointerLevels++;
	}

	public void setArray(int dimensions) {
		this.arrayDimensions = dimensions;
	}

	public int getArrayDimensions() {
		return arrayDimensions;
	}

	public void setClass() {
		isClass = true;
	}

	public void setComplex() {
		isComplex = true;
	}

	public void setEnum() {
		isEnum = true;
	}

	public void setPointer64() {
		isPointer64 = true;
	}

	public void setReference() {
		isReference = true;
	}

	public void setSigned() {
		isSigned = true;
	}

	public void setStruct() {
		isStruct = true;
	}

	public void setTemplate() {
		isTemplate = true;
	}

	public void setUnion() {
		isUnion = true;
	}

	public void setCoclass() {
		isCoclass = true;
	}

	public void setCointerface() {
		isCointerface = true;
	}

	public void setUnsigned() {
		isUnsigned = true;
	}

	public void setUnaligned() {
		isUnaligned = true;
	}

	public boolean isUnaligned() {
		return isUnaligned;
	}

	public void setVarArgs() {
		isVarArgs = true;
	}

//	public void setVolatile() {
//		isVolatile = true;
//	}
//
	public void setEnumType(String enumType) {
		this.enumType = enumType;
	}

	public void setRestrict() {
		isRestrict = true;
	}

	public boolean isRestrict() {
		return isRestrict;
	}

	public boolean isArray() {
		return arrayDimensions > 0;
	}

	public boolean isClass() {
		return isClass;
	}

	public boolean isComplex() {
		return isComplex;
	}

	public boolean isEnum() {
		return isEnum;
	}

	public boolean isPointer() {
		return pointerLevels > 0;
	}

	public boolean isPointer64() {
		return isPointer64;
	}

	public boolean isReference() {
		return isReference;
	}

	public boolean isSigned() {
		return isSigned;
	}

	public boolean isStruct() {
		return isStruct;
	}

	public boolean isTemplate() {
		return isTemplate;
	}

	public boolean isUnion() {
		return isUnion;
	}

	public boolean isCoclass() {
		return isCoclass;
	}

	public boolean isCointerface() {
		return isCointerface;
	}

	public boolean isUnsigned() {
		return isUnsigned;
	}

	public boolean isVarArgs() {
		return isVarArgs;
	}

	public boolean isVoid() {
		return VOID.equals(getName());
	}

//	public boolean isVolatile() {
//		return isVolatile;
//	}
//
	public String setEnumType() {
		return enumType;
	}

	public String getBasedName() {
		return basedName;
	}

	public void setBasedName(String basedName) {
		this.basedName = basedName;
	}

	public String getMemberScope() {
		return memberScope;
	}

	public void setMemberScope(String memberScope) {
		this.memberScope = memberScope;
	}

	public boolean isPrimitive() {
		boolean isPrimitiveDT =
			!isArray() && !isClass && !isComplex && !isEnum && !isPointer() && !isPointer64 &&
				!isSigned && !isTemplate && !isUnion && !isCoclass && !isCointerface && !isVarArgs;
//		boolean isPrimitiveDT = !isArray && !isClass && !isComplex && !isEnum && !isPointer() &&
//			!isPointer64 && !isSigned && !isTemplate && !isUnion && !isVarArgs;
//		boolean isPrimitiveDT = !isArray && !isClass && !isComplex && !isEnum && !isPointer() &&
//			!isPointer64 && !isSigned && !isTemplate && !isUnion && !isVarArgs && !isVolatile;
		if (isPrimitiveDT) {
			for (String primitiveNames : PRIMITIVES) {
				if (getName().equals(primitiveNames)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toSignature() {
		StringBuffer buffer = new StringBuffer();

		if (isUnion) {
			buffer.append(UNION + SPACE);
		}
		if (isStruct) {
			buffer.append(STRUCT + SPACE);
		}
		if (isEnum) {
			buffer.append(ENUM + SPACE);
			if ((enumType != null) && !("int".equals(enumType))) {
				buffer.append(enumType + SPACE);
			}
		}
		if (isClass) {
			buffer.append(CLASS + SPACE);
		}
		if (isCoclass) {
			buffer.append(COCLASS + SPACE);
		}
		if (isCointerface) {
			buffer.append(COINTERFACE + SPACE);
		}
		if (isComplex) {
			buffer.append(COMPLEX + SPACE);
		}
//		if (isVolatile) {
//			buffer.append(VOLATILE + SPACE);
//		}
		if (isSigned) {
			buffer.append(SIGNED + SPACE);
		}
		if (isUnsigned) {
			buffer.append(UNSIGNED + SPACE);
		}

		if (getNamespace() != null) {
			buffer.append(getNamespace().toNamespace());
		}

		buffer.append(getDemangledName());

		if (getTemplate() != null) {
			buffer.append(getTemplate().toTemplate());
		}

		if (isConst()) {
			buffer.append(SPACE + CONST);
		}

		// TODO: The output of volatile belongs here, not above, so I put the commented code here for now.
		if (isVolatile()) {
			buffer.append(SPACE + VOLATILE);
		}

		if (basedName != null) {
			buffer.append(SPACE + basedName);
		}

		if ((memberScope != null) && (memberScope.length() != 0)) {
			buffer.append(SPACE + memberScope + "::");
		}

		if (isUnaligned) {
			buffer.append(SPACE + UNALIGNED);
		}

		if (pointerLevels >= 1) {
			buffer.append(SPACE + PTR_NOTATION);
		}

		if (isReference) {
			buffer.append(SPACE + REF_NOTATION);
		}

		//Order of __ptr64 and __restrict can vary--with fuzzing... but what is the natural "real symbol" order?
		if (isPointer64) {
			buffer.append(SPACE + PTR64);
		}

		if (isRestrict) {
			buffer.append(SPACE + RESTRICT);
		}

		for (int i = 1; i < pointerLevels; i++) {
			buffer.append(SPACE + PTR_NOTATION);
		}

		if (isArray()) {
			// only put subscript on if the name doesn't have it
			Matcher matcher = ARRAY_SUBSCRIPT_PATTERN.matcher(getName());
			if (!matcher.find()) {
				for (int i = 0; i < arrayDimensions; i++) {
					buffer.append(ARR_NOTATION);
				}
			}
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		return toSignature();
	}

}
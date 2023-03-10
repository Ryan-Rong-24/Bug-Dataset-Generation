/**
 * Copyright (C) 2006-2018 INRIA and contributors Spoon - http://spoon.gforge.inria.fr/
 *
 * This software is governed by the CeCILL-C License under French law and abiding by the rules of
 * distribution of free software. You can use, modify and/or redistribute the software under the
 * terms of the CeCILL-C license as circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C
 * license and that you accept its terms.
 */
package spoon.test.ctType;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import spoon.FluentLauncher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.NamedElementFilter;
import spoon.test.ctType.testclasses.ErasureModelA;
import spoon.testing.utils.ModelUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CtTypeParameterTest {

	@Test
	public void testTypeErasure() throws Exception {
		//contract: the type erasure computed by getTypeErasure is same as the one computed by the Java compiler
		CtClass<?> ctModel = (CtClass<?>) ModelUtils.buildClass(ErasureModelA.class);
		//visit all methods of type ctModel
		//visit all inner types and their methods recursively `getTypeErasure` returns expected value
		//for each formal type parameter or method parameter check if
		checkType(ctModel);
	}

	private void checkType(CtType<?> type) throws NoSuchFieldException, SecurityException {
		List<CtTypeParameter> formalTypeParameters = type.getFormalCtTypeParameters();
		for (CtTypeParameter ctTypeParameter : formalTypeParameters) {
			checkTypeParamErasureOfType(ctTypeParameter, type.getActualClass());
		}

		for (CtTypeMember member : type.getTypeMembers()) {
			if (member instanceof CtFormalTypeDeclarer) {
				CtFormalTypeDeclarer ftDecl = (CtFormalTypeDeclarer) member;
				formalTypeParameters = ftDecl.getFormalCtTypeParameters();
				if (member instanceof CtExecutable<?>) {
					CtExecutable<?> exec = (CtExecutable<?>) member;
					for (CtTypeParameter ctTypeParameter : formalTypeParameters) {
						checkTypeParamErasureOfExecutable(ctTypeParameter);
					}
					for (CtParameter<?> param : exec.getParameters()) {
						checkParameterErasureOfExecutable(param);
					}
				} else if (member instanceof CtType<?>) {
					CtType<?> nestedType = (CtType<?>) member;
					// recursive call for nested type
					checkType(nestedType);
				}
			}
		}
	}

	private void checkTypeParamErasureOfType(CtTypeParameter typeParam, Class<?> clazz) throws NoSuchFieldException, SecurityException {
		Field field = clazz.getDeclaredField("param" + typeParam.getSimpleName());
		assertEquals(field.getType().getName(), typeParam.getTypeErasure().getQualifiedName(), "TypeErasure of type param " + getTypeParamIdentification(typeParam));
	}

	private void checkTypeParamErasureOfExecutable(CtTypeParameter typeParam) throws SecurityException {
		CtExecutable<?> exec = (CtExecutable<?>) typeParam.getParent();
		CtParameter<?> param = exec.filterChildren(new NamedElementFilter<>(CtParameter.class, "param" + typeParam.getSimpleName())).first();
		assertNotNull(param, "Missing param" + typeParam.getSimpleName() + " in " + exec.getSignature());
		int paramIdx = exec.getParameters().indexOf(param);
		Class declClass = exec.getParent(CtType.class).getActualClass();
		Executable declExec;
		if (exec instanceof CtConstructor) {
			declExec = declClass.getDeclaredConstructors()[0];
		} else {
			declExec = getMethodByName(declClass, exec.getSimpleName());
		}
		Class<?> paramType = declExec.getParameterTypes()[paramIdx];
		// contract the type erasure given with Java reflection is the same as the one computed by spoon
		assertEquals(paramType.getTypeName(), param.getType().getTypeErasure().toString(), "TypeErasure of executable param " + getTypeParamIdentification(typeParam));
	}

	private void checkParameterErasureOfExecutable(CtParameter<?> param) {
		CtExecutable<?> exec = param.getParent();
		CtTypeReference<?> typeErasure = param.getType().getTypeErasure();
		int paramIdx = exec.getParameters().indexOf(param);
		Class declClass = exec.getParent(CtType.class).getActualClass();
		Executable declExec;
		if (exec instanceof CtConstructor) {
			declExec = declClass.getDeclaredConstructors()[0];
		} else {
			declExec = getMethodByName(declClass, exec.getSimpleName());
		}
		Class<?> paramType = declExec.getParameterTypes()[paramIdx];
		assertEquals(0, typeErasure.getActualTypeArguments().size());
		// contract the type erasure of the method parameter given with Java reflection is the same as the one computed by spoon
		assertEquals(paramType.getTypeName(), typeErasure.getQualifiedName(), "TypeErasure of executable " + exec.getSignature() + " parameter " + param.getSimpleName());
	}

	private Executable getMethodByName(Class declClass, String simpleName) {
		for (Method method : declClass.getDeclaredMethods()) {
			if (method.getName().equals(simpleName)) {
				return method;
			}
		}
		fail("Method " + simpleName + " not found in " + declClass.getName());
		return null;
	}

	private String getTypeParamIdentification(CtTypeParameter typeParam) {
		String result = "<" + typeParam.getSimpleName() + ">";
		CtFormalTypeDeclarer l_decl = typeParam.getParent(CtFormalTypeDeclarer.class);
		if (l_decl instanceof CtType) {
			return ((CtType) l_decl).getQualifiedName() + result;
		}
		if (l_decl instanceof CtExecutable) {
			CtExecutable exec = (CtExecutable) l_decl;
			if (exec instanceof CtMethod) {
				result = exec.getSignature() + result;
			}
			return exec.getParent(CtType.class).getQualifiedName() + "#" + result;
		}
		throw new AssertionError();
	}

	@Test
	public void testTypeSame() throws Exception {
		CtClass<?> ctModel = (CtClass<?>) ModelUtils.buildClass(ErasureModelA.class);
		CtTypeParameter tpA = ctModel.getFormalCtTypeParameters().get(0);
		CtTypeParameter tpB = ctModel.getFormalCtTypeParameters().get(1);
		CtTypeParameter tpC = ctModel.getFormalCtTypeParameters().get(2);
		CtTypeParameter tpD = ctModel.getFormalCtTypeParameters().get(3);

		CtConstructor<?> ctModelCons = ctModel.getConstructors().iterator().next();
		CtMethod<?> ctModelMethod = ctModel.getMethodsByName("method").get(0);
		CtMethod<?> ctModelMethod2 = ctModel.getMethodsByName("method2").get(0);

		CtClass<?> ctModelB = ctModel.filterChildren(new NamedElementFilter<>(CtClass.class, "ModelB")).first();
		CtTypeParameter tpA2 = ctModelB.getFormalCtTypeParameters().get(0);
		CtTypeParameter tpB2 = ctModelB.getFormalCtTypeParameters().get(1);
		CtTypeParameter tpC2 = ctModelB.getFormalCtTypeParameters().get(2);
		CtTypeParameter tpD2 = ctModelB.getFormalCtTypeParameters().get(3);

		CtConstructor<?> ctModelBCons = ctModelB.getConstructors().iterator().next();
		CtMethod<?> ctModelBMethod = ctModelB.getMethodsByName("method").get(0);

		//the type parameters of ErasureModelA and ErasureModelA$ModelB are same if they are on the same position.
		checkIsSame(ctModel.getFormalCtTypeParameters(), ctModelB.getFormalCtTypeParameters(), true);

		//the type parameters of ErasureModelA#constructor and ErasureModelA$ModelB constructor are same, because constructors has same formal type parameters
		//https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.4
		checkIsSame(ctModelCons.getFormalCtTypeParameters(), ctModelBCons.getFormalCtTypeParameters(), true);
		//the type parameters of ctModel ErasureModelA#method and ErasureModelA$ModelB#method are same if they are on the same position.
		checkIsSame(ctModelMethod.getFormalCtTypeParameters(), ctModelBMethod.getFormalCtTypeParameters(), true);

		//the type parameters of ctModel ErasureModelA#constructor and ErasureModelA$ModelB#method are never same, because they have different type of scope (Method!=Constructor)
		checkIsSame(ctModelCons.getFormalCtTypeParameters(), ctModelBMethod.getFormalCtTypeParameters(), false);
		//the type parameters of ctModel ErasureModelA#method and ErasureModelA#method2 are same, because they have same formal type parameters
		//https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.4
		checkIsSame(ctModelMethod.getFormalCtTypeParameters(), ctModelMethod2.getFormalCtTypeParameters(), true);

		CtClass<?> ctModelC = ctModel.filterChildren(new NamedElementFilter<>(CtClass.class, "ModelC")).first();
	}

	/**
	 * checks that parameters on the same position are same and parameters on other positions are not same
	 *
	 * @param isSameOnSameIndex TODO
	 */
	private void checkIsSame(List<CtTypeParameter> tps1, List<CtTypeParameter> tps2, boolean isSameOnSameIndex) {
		for (int i = 0; i < tps1.size(); i++) {
			CtTypeParameter tp1 = tps1.get(i);
			for (int j = 0; j < tps2.size(); j++) {
				CtTypeParameter tp2 = tps2.get(j);
				if (i == j && isSameOnSameIndex) {
					checkIsSame(tp1, tp2);
				} else {
					checkIsNotSame(tp1, tp2);
				}
			}
		}
	}

	private void checkIsSame(CtTypeParameter tp1, CtTypeParameter tp2) {
		assertTrue(isSame(tp1, tp2, false, true) || isSame(tp2, tp1, false, true));
	}

	private void checkIsNotSame(CtTypeParameter tp1, CtTypeParameter tp2) {
		assertFalse(isSame(tp1, tp2, false, true) || isSame(tp2, tp1, false, true));
	}

	private static boolean isSame(CtTypeParameter thisType, CtTypeParameter thatType, boolean canTypeErasure, boolean checkMethodOverrides) {
		CtTypeReference<?> thatAdaptedType = thisType.getFactory().Type().createTypeAdapter(thisType.getTypeParameterDeclarer()).adaptType(thatType);
		if (thatAdaptedType == null) {
			return false;
		}
		return thisType.getQualifiedName().equals(thatAdaptedType.getQualifiedName());
	}

	@Test
	public void test() {
		// testcase for issue 3275. The issue was a nullpointer in this code. Was fixed in #3276
		new FluentLauncher()
			.inputResource("src/test/resources/issue3275/BOMCostPrice.java")
			.noClasspath(true)
			.processor(new AbstractProcessor<CtTypeReference<?>>() {
					public void process(CtTypeReference<?> element) {
						// just a operation to enforce the method 
					String a = element.getSimpleName();
				}
			})
			.buildModel();
	}
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirReturnExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject

internal fun KtClassOrObject.generateComponentFunctions(
    session: FirSession, firClass: FirClassImpl, packageFqName: FqName, classFqName: FqName
) {
    var componentIndex = 1
    val zippedParameters =
        primaryConstructorParameters.zip(firClass.declarations.filterIsInstance<FirProperty>())
    for ((ktParameter, firProperty) in zippedParameters) {
        if (!ktParameter.hasValOrVar()) continue
        val name = Name.identifier("component$componentIndex")
        componentIndex++
        val symbol = FirFunctionSymbol(CallableId(packageFqName, classFqName, name))
        firClass.addDeclaration(
            FirMemberFunctionImpl(
                session, ktParameter, symbol, name,
                Visibilities.PUBLIC, Modality.FINAL,
                isExpect = false, isActual = false,
                isOverride = false, isOperator = false,
                isInfix = false, isInline = false,
                isTailRec = false, isExternal = false,
                isSuspend = false, receiverTypeRef = null,
                returnTypeRef = FirImplicitTypeRefImpl(session, ktParameter)
            ).apply {
                val componentFunction = this
                body = FirSingleExpressionBlock(
                    session,
                    FirReturnExpressionImpl(
                        session, ktParameter,
                        FirQualifiedAccessExpressionImpl(session, ktParameter).apply {
                            val parameterName = ktParameter.nameAsSafeName
                            calleeReference = FirResolvedCallableReferenceImpl(
                                session, ktParameter,
                                parameterName, firProperty.symbol
                            )
                        }
                    ).apply {
                        target = FirFunctionTarget(null)
                        target.bind(componentFunction)
                    }
                )
            }
        )
    }
}
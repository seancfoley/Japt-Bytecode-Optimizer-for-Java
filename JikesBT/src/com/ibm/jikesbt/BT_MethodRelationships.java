package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/*
 Implementation notes:
 - The words "attach" and "detach" are generally used for
   consistency-preserving public methods, whereas "link",
   "delink", and "relink" are generally not consistency-preserving
   and are private.
 * @author IBM
*/

/**
 A group of static methods that handle most updates of the
 inter-method relationships (parents, kids, and inlaws).
 The code got too bulky to be in BT_Class or BT_Method.

 <p> For the inspiration of the design, see "Design observations
 8/30/1999" at methods BT_Class.attachParent,
 BT_Class.detachParent and BT_Method.resetDeclaringClassAndName.

**/
final class BT_MethodRelationships extends BT_Base {

	private BT_MethodRelationships() {
		throw new RuntimeException();
	}

	// ----------------------------------------------------------------------------
	// :h- Misc Services

	/**
	 Searches the tree using field BT_Class.parents_ -- ignores field BT_Method.parents_.
	 @param  candCs  The candidate classes to start searching at.
	**/
//	private static boolean isReallyParent(
//		BT_Method frM,
//		BT_Method targetM,
//		BT_ClassVector candCs) {
//		for (int i = 0; i < candCs.size(); ++i) { // Per immediate parent
//			BT_Class candC = candCs.elementAt(i);
//			BT_Method candM = candC.findMethodOrNull(frM.name, frM.signature);
//			if (candM == targetM) // Found it
//				return true;
//			if (candM == null) { // Found no parent method
//				if (isReallyParent(frM, targetM, candC.parents_)) // Found it
//					return true;
//			} // Found no parent method
//		} // Per immediate parent
//		return false;
//	}

	/**
	 Generates a set of the methods that can be inherited from this
	 this class|interface including:
	 - those in the class|interface itself,
	 - those it inherits from its superclasses, and
	 - those in interfaces that it implements directly or indirectly (by inheriting from a superclass).
	 <p>
	 Does not include:
	 - those in interfaces that it implements directly,
	 - those that are overridden by other methods.
	 <p>
	 This is private because its definition is a bit wierd.
	
	 <p> The collection itself returned is not part of JikesBT.
	 See <a href=../jikesbt/doc-files/ProgrammingPractices.html#returned_references>returned references</a>.
	**/
	private static BT_MethodVector findAllMethodsWhichCanBeInheritedByAClass(BT_Class ac) {
		BT_MethodVector ms = new BT_MethodVector();

		for (int im = 0;
			im < ac.methods.size();
			++im) // Per method directly in ac class
			if (ac.methods.elementAt(im).canBeInherited())
				ms.addElement(ac.methods.elementAt(im));

		for (int ipc = 0;
			ipc < ac.parents_.size();
			++ipc) { // Per parent class
			BT_MethodVector pcs =
				findAllMethodsWhichCanBeInheritedByAClass(
					ac.parents_.elementAt(ipc));
			for (int im = 0;
				im < pcs.size();
				++im) { // Per method in the parent closure
				if (null
					== ac.findMethodOrNull(
						pcs.elementAt(im).name,
						pcs.elementAt(im).getSignature())) // Not overridden
					ms.addElement(pcs.elementAt(im));
			} // Per method in the parent closure
		} // Per parent class
		return ms;
	}
	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Parents & Kids -- Delinking

	/**
	 Not a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	**/
	private static void delinkParent(BT_Method kidM, BT_Method parM) {
		if (CHECK_JIKESBT && !kidM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! kidM.getDeclaringClass().getRepository().factory.buildMethodRelationships");
		if (CHECK_USER && kidM == null)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "kidM != null"));
		if (CHECK_USER && parM == null)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "parM != null"));

		boolean removedP = kidM.parents.removeElement(parM);
		// Remove parM<-thisC
		boolean removedK = parM.kids.removeElement(kidM);
		// Remove parM->thisC

		if (CHECK_USER && !removedP)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "removedP")); // Expect back-pointer
		if (CHECK_USER && !removedK)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "removedK")); // Expect back-pointer
	}

	/**
	 Not a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	**/
	static void delinkParents(BT_Method kidM) {
		if (CHECK_JIKESBT && !kidM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! kidM.getDeclaringClass().getRepository().factory.buildMethodRelationships");

		for (int i = 0; i < kidM.parents.size(); ++i) {
			BT_Method parent = kidM.parents.elementAt(i);
			if(BT_Factory.multiThreadedLoading) {
				parent.methodKidsLock.lock();
			}
			parent.kids.removeElement(kidM);
			if(BT_Factory.multiThreadedLoading) {
				parent.methodKidsLock.unlock();
			}
				
		}
		kidM.parents.removeAllElements();
	}

	/**
	 Not a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships BT_Factory.factory.buildMethodRelationships} is true.
	**/
	static void delinkKids(BT_Method parM) {
		if (CHECK_JIKESBT && !parM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! parM.getDeclaringClass().getRepository().factory.buildMethodRelationships");

		for (int i = 0; i < parM.kids.size(); ++i)
			parM.kids.elementAt(i).parents.removeElement(parM);
		parM.kids.removeAllElements();
	}

	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Parents & Kids -- Linking

	/**
	 Links a parent to a kid, and deletes any inlaw obsoleted by this linkage.
	**/
	private static void linkParentAndKid(BT_Method parM, BT_Method kidM) {
		if (CHECK_USER_THOROUGHLY && !parM.canBeInherited())
			expect(Messages.getString("JikesBT.{0}_cannot_be_inherited____parent__{1}_12", new Object[] {parM, kidM}));
		if (CHECK_USER_THOROUGHLY && !kidM.canInherit())
			expect(Messages.getString("JikesBT.{0}_cannot_inherit____parM__{1}_13", new Object[] {kidM, parM}));
		if (parM.addKid(kidM)) { // Was added -- didn't already exist
			boolean added = kidM.addParent(parM);
			if (CHECK_USER && !added)
				expect(Messages.getString("JikesBT.Failed___{0}_22", "added"));
			delinkInlawsWhichAreNowInherited(parM, kidM);
		}
	}

	/**
	 Links this "kid" method with the same-named method in an ancestor
	 class.
	 Assumes the kid can override the parent?
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	 @param  pc  The ancestor class to start searching at.
	**/
	private static void linkParentsInAClassOrAncestors(
		BT_Method frM,
		BT_Class pc) {
		if (CHECK_JIKESBT && !pc.getRepository().factory.buildMethodRelationships)
			assertFailure("! pc.getRepository().factory.buildMethodRelationships");
		if (CHECK_USER_THOROUGHLY && !frM.canInherit())
			expect(Messages.getString("JikesBT.{0}_cannot_inherit_16", frM));
		BT_Method pm = pc.findMethodOrNull(frM.name, frM.getSignature());
		
		// Matching parent method
		if (pm != null) {
			if (pm.canBeInherited()) {
				// In particular, parent.isPrivate is legal but doesn't not cause inheritence
				if(BT_Factory.multiThreadedLoading) {
					pm.methodKidsLock.lock();
				}
				linkParentAndKid(pm, frM);
				if(BT_Factory.multiThreadedLoading) {
					pm.methodKidsLock.unlock();
				}
			}
		} else // Not in parent -- try grandparent
			linkParentsInAncestorsOfClass(frM, pc);
	}
	
	/**
	 Links this "kid" method with the first-reached same-named methods in its
	 "parent" classes or else in their parents, etc.
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	**/
	private static void linkParentsInAncestorsOfClass(
		BT_Method frM,
		BT_Class ofC) {
		if (CHECK_JIKESBT && !ofC.getRepository().factory.buildMethodRelationships)
			assertFailure("! ofC.getRepository().factory.buildMethodRelationships");
		if (CHECK_USER_THOROUGHLY && !frM.canInherit())
			expect(Messages.getString("JikesBT.{0}_cannot_inherit_16", frM));
		for (int i = 0; i < ofC.parents_.size(); ++i)
			linkParentsInAClassOrAncestors(frM, ofC.parents_.elementAt(i));
	}

	/**
	 Assumes the method can inherit.
	**/
	static void linkParents(BT_Method frM) {
		if (CHECK_USER_THOROUGHLY && !frM.canInherit())
			expect(Messages.getString("JikesBT.{0}_cannot_inherit_16", frM));
		linkParentsInAncestorsOfClass(frM, frM.cls);
	}

	/**
	 Links this "parent" method with the same-named method in another
	 "kid" class (pc) or else in that class's kids, etc.
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships BT_Factory.factory.buildMethodRelationships} is true.
	 @param  frM  Must be able to be inherited.
	**/
	private static void linkKidsInOrDescendentsOfAClass(
		BT_Method frM,
		BT_Class desC) {
		if (CHECK_JIKESBT && !desC.getRepository().factory.buildMethodRelationships)
			assertFailure("! desC.getRepository().factory.buildMethodRelationships");
		if (CHECK_USER && !frM.canBeInherited())
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "frM.canBeInherited()"));
		BT_Method km = desC.findMethodOrNull(frM.name, frM.getSignature());
		// Matching kid method
		if (km != null) {
			if (FAIL_NORMALLY) { // Fail-hard (normal) version
				if (CHECK_USER && !km.canInherit())
					assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "km.canInherit()"));
				linkParentAndKid(frM, km);
			} else { // Fail-soft version for JikesBT testing
				if (km.canInherit()) // A normally-unneeded check
					linkParentAndKid(frM, km);
			}
		} else // Not in kid -- try grandkid
			linkKidsInDescendentsOfClass(frM, desC);
	}
	/**
	 Links this "parent" method with the first-reached same-named
	 methods in the kids of the parameter class.
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	 @param  frM  Must be able to be inherited.
	**/
	private static void linkKidsInDescendentsOfClass(
		BT_Method frM,
		BT_Class of) {
		if (CHECK_JIKESBT && of == null)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "of != null"));
		if (CHECK_JIKESBT && !of.getRepository().factory.buildMethodRelationships)
			assertFailure("! of.getRepository().factory.buildMethodRelationships");
		if (CHECK_USER_THOROUGHLY && !frM.canBeInherited())
			assertFailure(Messages.getString("JikesBT.{0}_cannot_be_inherited_4", frM));

		for (int i = 0; i < of.kids_.size(); ++i)
			linkKidsInOrDescendentsOfAClass(frM, of.kids_.elementAt(i));
	}

	/**
	 @param  frM  Must be able to be inherited.
	**/
//	private static void linkKids(BT_Method frM) {
//		linkKidsInDescendentsOfClass(frM, frM.cls);
//	}

	/**
	 This is more efficient than a general search for kids to add
	 because it considers only one path down from the conditional
	 parent initially.
	
	 <p> This is very similar to {@link BT_MethodRelationships#linkInlawsAlongDescendentPath}.
	
	 @param  ancestorC  The class whose methods may now become parents, and
	   whose ancestors' methods may similarly ....
	 @param  seenCs  The classes between ancestorC and thruDescendentC.
	 @param  thruDescendentC  The class that may have kids to be added.
	   Its descendents may also have kids to be added.
	**/
	static void linkKidsOfAncestorMethodsThruAClass(
		BT_Class ancestorC,
		BT_ClassVector seenCs,
		BT_Class thruDescendentC) {

		if (false)
			traceln(
				"lAMWKTGC  ancestorC: "
					+ ancestorC
					+ " #: "
					+ seenCs.size()
					+ " thruDescendentC: "
					+ thruDescendentC);
		perParentMethod : for (
			int iParM = 0;
				iParM < ancestorC.methods.size();
				++iParM) { // Per unsure method in ancestorC
			BT_Method parM = ancestorC.methods.elementAt(iParM);
			if (parM.canBeInherited()) { // It can have kids
				// If there is a same-named method between here and the target class, don't try to add descendents
				for (int iSeenC = 0;
					iSeenC < seenCs.size();
					++iSeenC) { // Per seen class
					BT_Class seenC = seenCs.elementAt(iSeenC);
					if (seenC.findMethodOrNull(parM.name, parM.getSignature())
						!= null)
						continue perParentMethod;
					// parM has no kid via thruDescendentC
				} // Per seen class
				if (false)
					traceln(
						"lAMWKTGC  may link thru "
							+ thruDescendentC
							+ " from ancestor method: "
							+ parM);
				linkKidsInOrDescendentsOfAClass(parM, thruDescendentC);
			} // It can have kids
		} // Per unsure method in ancestorC

		seenCs.addElement(ancestorC); // Push
		BT_ClassVector pcs = ancestorC.parents_;
		for (int ipc = 0; ipc < pcs.size(); ++ipc) // Per parent of current
			linkKidsOfAncestorMethodsThruAClass(
				pcs.elementAt(ipc),
				seenCs,
				thruDescendentC);
		seenCs.removeElementAt(seenCs.size() - 1); // Pop
	}

	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Parents & Kids -- Relinking

	/**
	 Corrects (unlinks and links as necessary) the parents in the methods of subclasses/subinterfaces.
	**/
	private static class RelinkDescendentMethodsCDV
		extends BT_ClassDescendentVisitor {
		private final int copiedCurMark_; // A parameter from the caller (ugh)
		private boolean pathBranches_;
		private Boolean someBranches_;
		// A result to the caller (ugh) -- Do I or some descendent have >1 parent?
		RelinkDescendentMethodsCDV(int curMark, boolean parentBranches) {
			super();
			this.copiedCurMark_ = curMark;
			this.pathBranches_ = parentBranches;
			this.someBranches_ = parentBranches ? Boolean.TRUE : Boolean.FALSE;
		}
		public void visit(BT_Class dc) { // Per descendent (or the original) class
			dc.tempMark_ = copiedCurMark_; // A bread-crumb
			boolean saveBranches = this.pathBranches_; // Save
			if (dc.parents_.size() > 1) { // This class has >1 parent
				this.pathBranches_ = true;
				// There is >1 parent somewhere in this linear path
				this.someBranches_ = Boolean.TRUE;
				// There is >1 parent somewhere
			}
			for (int idm = 0;
				idm < dc.methods.size();
				++idm) { // Per method in the descendent
				BT_Method dm = dc.methods.elementAt(idm);
				if (dm.canInherit()) { // Descendent can inherit
					if (this.pathBranches_) {
						delinkParents(dm);
						if (CHECK_JIKESBT && dm.parents.size() != 0)
							assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "dm.parents_.size() == 0"));
						linkParents(dm);
					} else { // Special case
						// This descendent's parents must be directly down the path from thisC or from thisC or above thisC (i.e., not around thisC).
						BT_MethodVector dpms = dm.parents;
						for (int idpm = dpms.size() - 1;
							idpm != -1;
							--idpm) { // Per descendent's parent method -- must loop backward!
							BT_Method dpm = dpms.elementAt(idpm);
							if (dpm.cls.tempMark_ != copiedCurMark_) {
								// Descendent's parent is in my former ancestor
								delinkParent(dm, dpm); // Bye
							} // Descendent's parent is in my former ancestor
						} // Per descendent's parent method
					}
				} // Descendent can inherit
			} // Per method in the descendent
			super.visit(dc); // Continue further down
			this.pathBranches_ = saveBranches; // Restore
		}
		public Object visitAClassAndDescendents(BT_Class dc) { // Per descendent (or the original) class
			super.visitAClassAndDescendents(dc);
			return someBranches_;
		}
	}

	/**
	 @return  True if the class of this method or some descendent of it has >1 parent.
	**/
	static boolean relinkParentsOfMyAndDescendentMethods(BT_Class thisC) {
		++BT_Class.curMark_;
		int save = BT_Class.curMark_;
		RelinkDescendentMethodsCDV visitor =
			new RelinkDescendentMethodsCDV(
				BT_Class.curMark_,
				thisC.parents_.size() != 0);
		// Reminder: parents_ has already been updated
		Boolean someBranches =
			(Boolean) visitor.visitAClassAndDescendents(thisC);
		// Visit _me_ and my descendents
		if (CHECK_JIKESBT && save != BT_Class.curMark_)
			assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "save==BT_Class.curMark_"));
		return someBranches.booleanValue();
	}

	/**
	 Detach the old parents & kids from deldM and attach them to each other.
	**/
	static void relinkParentsAndKidsOfDeletedMethod(final BT_Method deldM) {
		for (int ipm = 0; ipm < deldM.parents.size(); ++ipm) { // Per parent
			BT_Method parM = deldM.parents.elementAt(ipm);
			boolean found = parM.kids.removeElement(deldM);
			// Remove 1 ptr parent->deldM
			if (CHECK_JIKESBT && !found)
				expect(Messages.getString("JikesBT.Failed___{0}_22", "found"));
			for (int ikm = 0; ikm < deldM.kids.size(); ++ikm) // Per kid
				linkParentAndKid(parM, deldM.kids.elementAt(ikm));
			// Add 1 link parent<->kid
		} // Per parent
		deldM.parents.removeAllElements(); // Remove all ptrs deldM->parents
		delinkKids(deldM);
	}

	/**
	 Find the new parents & kids, detach them from each other, and attach them to addedM.
	**/
	static void relinkParentsAndKidsOfAddedMethod(final BT_Method addedMethod) {
		// if(trace_) traceln( "relinkParentsAndKidsOfAddedMethod " + addedM);
		// - Adding thisC
		//   - For each new kid
		//     - If all branching==1
		//       - Just interpose thisC between it and each of its parents!
		//     - Else
		//       - Delink it from each of its parents
		//       - Link it to each of its parents
		BT_ClassDescendentVisitor cdv = new BT_ClassDescendentVisitor() {
			private boolean pathBranches_ = false;
			// Does some descendent have >1 parent?
			public void visit(BT_Class dc) { // Per descendent class
				boolean saveBranches = pathBranches_; // Save
				if (dc.parents_.size() != 1) {
					pathBranches_ = true;
				}
				BT_Method childMethod =
					dc.findMethodOrNull(addedMethod.name, addedMethod.getSignature());
				if (childMethod != null) {
					// Found such a descendent method in addedM class
					if (childMethod.canInherit()) {
						if (pathBranches_ || !addedMethod.canBeInherited()) {
							// if(trace_) traceln( "... yes branches " + dm);
							delinkParents(childMethod);
							if (CHECK_JIKESBT && childMethod.parents.size() != 0)
								assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "dm.parents_.size() == 0"));
							linkParents(childMethod);
						} else { // Special case
							// This descendent's parents must now be mine
							for (int parentOfChildIndex = 0; parentOfChildIndex < childMethod.parents.size(); ++parentOfChildIndex) { // Per parent of a descendent method (formerly his, now mine)
								
								//for each parent, I find myself in the list of kids
								BT_Method parentOfChild = childMethod.parents.elementAt(parentOfChildIndex);
								BT_MethodVector parentOfChildKids = parentOfChild.kids;
								int ii = parentOfChildKids.indexOf(childMethod);
								
								if (CHECK_USER && ii == -1)
									assertFailure(Messages.getString("JikesBT.Failed___{0}_22", "Parent did not point to kid"));
								
								boolean added = addedMethod.addParent(parentOfChild);
								
								// Point pdm<-thisC
								if (!added || !parentOfChild.replaceKid(addedMethod, ii)) {
									parentOfChildKids.removeElementAt(ii);
								}// Detach duplicate pdm->dm
							}
							// This descendent's only new parent must be thisC.
							childMethod.parents.removeAllElements();
							// Remove all par<-dms
							linkParentAndKid(addedMethod, childMethod); // Link thisC<->dm
						}
					}
				} else { // No such descendent method in addedM class
					super.visit(dc); // Continue further down
				}
				pathBranches_ = saveBranches; // Restore
			}
		};
		// For performance, make use of someBranches_
		cdv.visitDescendents(addedMethod.cls);

		//   - If all kids were reached w/ branching==1
		//     - Done
		//   - Else
		//     - Link thisC to my parents w/o need to delete existing links first
		if (addedMethod.canInherit())
			linkParents(addedMethod);

		// For performance, add kids same time as computing inlaws?
	}

	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Inlaws -- Delinking

	/**
	 Unlinks a BT_MethodInlaw from both its methods.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	 @param  frIx  The index of the BT_InlawPair to be deleted from frM.
	**/
	static void delinkInlawPairByIndex(BT_Method frM, int frIx) {
		if (CHECK_JIKESBT && !frM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! frM.getDeclaringClass().getRepository().factory.buildMethodRelationships");
		BT_MethodInlaw pair1 = frM.inlaws.elementAt(frIx);
		// if(trace_) traceln( "delinkInlawPair -- " + pair1.getOtherMethod(frM) + " cause: " + pair1.cls_ + " ix: " + frIx);
		BT_Class causeC = pair1.cls_;
		BT_Method m2 = pair1.getOtherMethod(frM);
		m2.inlaws.depointInlaw(causeC, frM);
		frM.inlaws.removeElementAt(frIx);
	}

	/**
	 Sort of a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>,
	 except that it does its job (delinking that either corrects or damages consistency, or leaves it damaged).
	 Assumes back-pointers are consistent.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships BT_Factory.factory.buildMethodRelationships} is true.
	**/
	static void delinkInlawsOfMethod(BT_Method frM) {
		if (false)
			traceln("delinkInlawsOfMethod -- " + frM);
		if (CHECK_JIKESBT && !frM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! frM.getDeclaringClass().getRepository().factory.buildMethodRelationships");

		for (int i = 0; i < frM.inlaws.size(); ++i) { // Per inlaw record
			BT_MethodInlaw thisPair = frM.inlaws.elementAt(i);
			BT_Method m2 = thisPair.getOtherMethod(frM);
			m2.inlaws.depointInlaw(thisPair.cls_, frM); // De-point me <- him
		}

		frM.inlaws.removeAllElements(); // De-point me -> him
	}

	/**
	 Deletes inlaw records that have become obsolete because the two inlaw methods are now directly related.
	 @param  parM  A new parent.
	**/
	private static void delinkInlawsWhichAreNowInherited(
		BT_Method parM,
		BT_Method kidM) {
		if (CHECK_JIKESBT && !parM.isParentOf(kidM))
			assertFailure(Messages.getString("JikesBT.Oops__{0}______{1}_48", new Object[] {parM, kidM}));
		for (int iPair = parM.inlaws.size() - 1;
			iPair != -1;
			--iPair) { // Per method inlaw record
			BT_MethodInlaw pair = parM.inlaws.elementAt(iPair);
			if (pair.getOtherMethod(parM) == kidM) {
				// if(trace_) traceln( "delinkInlawsWhichAreNowInherited delinking: " + parM + " & " + pair.getOtherMethod(parM));
				pair.getOtherMethod(parM).inlaws.depointInlaw(pair.cls_, parM);
				// Depoint here <- there
				parM.inlaws.removeElementAt(iPair); // Depoint here -> there
			}
		}
	}

	/**
	 @see #linkInlawsOfAllMethodsOfClassAndAncestors
	**/
	static void delinkInlawsOfAllMethodsOfClassAndAncestors(BT_Class startC) {
		// if(trace_) traceln( "delinkInlawsOfAllMethodsOfClassAndAncestors -- " + startC);
		for (int im = 0;
			im < startC.methods.size();
			++im) // Per parent class method
			delinkInlawsOfMethod(startC.methods.elementAt(im));
		for (int ipc = 0;
			ipc < startC.parents_.size();
			++ipc) { // Per parent class
			BT_Class pc = startC.parents_.elementAt(ipc);
			delinkInlawsOfAllMethodsOfClassAndAncestors(pc);
		}
	}
	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Inlaws -- Linking

	/**
	 Link a pair of corresponding inlaws with each other -- if they
	 satisfy simple checks and if they aren't already linked.
	 Marks the new or old pair.
	**/
	private static void linkInlawPairIf(
		BT_Method sub,
		BT_Method m2,
		BT_Class causeC) {
		// (checks are in the BT_MethodInlaw ctor)

		if (sub != m2
			&& !m2.parents.contains(sub)
			&& !sub.parents.contains(m2)) {

			BT_MethodInlaw pair = sub.inlaws.findInlawRecord(m2, causeC);
			if (pair == null) {
				pair = new BT_MethodInlaw(sub, m2, causeC);
				if(BT_Factory.multiThreadedLoading) {
					sub.methodKidsLock.lock();
				}
				sub.inlaws.addElement(pair);
				if(BT_Factory.multiThreadedLoading) {
					sub.methodKidsLock.unlock();
				}
				if(BT_Factory.multiThreadedLoading) {
					m2.methodKidsLock.lock();
				}
				m2.inlaws.addElement(pair);
				if(BT_Factory.multiThreadedLoading) {
					m2.methodKidsLock.unlock();
				}
			} else
				pair.tempMark_ = BT_MethodInlaw.curMark_; // Remember was found, used by relinkInlawsOfAddedMethod only
		}
	}

	/**
	 Handles recursion up thru a marriage's ancestors for {@link BT_MethodRelationships#linkInlawsOfAMethod}
	 (from the common class up to the searched-for inlaw methods).
	 @param  causeC  The common class -- the one that inherits|implements both methods.
	 @param  exceptC  Null or the class that is not to be searched (because
	   that's where we came from.
	 @param  inlawCs  The classes that may contain an inlaw method or
	   whose ancestors, ... may.
	**/
	private static void linkInlawsOfAMethod_AndCausedByAClass_WithMethodsInAClassesOrTheirAncestors(
		BT_Method origM,
		BT_Class causeC,
		BT_Class exceptC,
		BT_ClassVector inlawCs) {
		if (CHECK_JIKESBT && !origM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! origM.getDeclaringClass().getRepository().factory.buildMethodRelationships");
		if (CHECK_JIKESBT_THOROUGHLY
			&& !(exceptC == null || causeC.isKidOf(exceptC)))
			assertFailure(Messages.getString("JikesBT.{0}_should_be_a_parent_of_{1}_51", new Object[] {exceptC, causeC}));

		for (int iInlawC = inlawCs.size() - 1;
			iInlawC != -1;
			--iInlawC) { // Per parent class
			BT_Class inlawC = inlawCs.elementAt(iInlawC);
			if (inlawC != exceptC) { // Not back to origin
				BT_Method inlawM =
					inlawC.findMethodOrNull(origM.name, origM.getSignature());
				if (inlawM != null) { // Found an possible inlaw
					if (inlawM.canBeInherited())
						linkInlawPairIf(origM, inlawM, causeC);
				} // Found an inlaw
				else { // No match in this parent
					linkInlawsOfAMethod_AndCausedByAClass_WithMethodsInAClassesOrTheirAncestors(
						origM,
						causeC,
						null,
						inlawC.parents_);
					// Recurse
				} // No match in this parent
			} // Not back to origin
		} // Per parent class
	}
	/**
	 Handles recursion down thru descendents for {@link BT_MethodRelationships#linkInlawsOfAMethod}.
	 @param  causeAncestorC  A candidate to be a causing class, or the ancestor of one.
	**/
	private static void linkInlawsOfAMethod_AndCausedByDescendentsOfAClass(
		BT_Method origM,
		BT_Class causeAncestorC) {
		if (CHECK_JIKESBT && !origM.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! origM.getDeclaringClass().getRepository().factory.buildMethodRelationships");

		BT_ClassVector kids = causeAncestorC.kids_;
		for (int ikc = 0;
			ikc < kids.size();
			++ikc) // Per direct subinterface|subclass|implementing-class
			linkInlawsOfAMethod_AndCausedByAClassOrDescendents(
				origM,
				kids.elementAt(ikc),
				causeAncestorC);
	}

	/**
	 @param  exceptC  The class that _not_ to go up thru.
	   Should be the parent of causeC that we one came down thru.
	**/
	private static void linkInlawsOfAMethod_AndCausedByAClassOrDescendents(
		BT_Method origM,
		BT_Class causeC,
		BT_Class exceptC) {
		if (CHECK_JIKESBT_THOROUGHLY && !causeC.isKidOf(exceptC))
			assertFailure(Messages.getString("JikesBT.{0}_should_be_a_parent_of_{1}_51", new Object[] {exceptC, causeC}));

		BT_Method km = causeC.findMethodOrNull(origM.name, origM.getSignature());
		// Find any overriding|implementing method
		if (km == null) { // None, so it and descendents may cause an inlaw
			// See if there are inlaws with this common class (causeC)
			if (causeC.parents_.size() > 1) // Can cause inlaws
				linkInlawsOfAMethod_AndCausedByAClass_WithMethodsInAClassesOrTheirAncestors(
					origM,
					causeC,
					exceptC,
					causeC.parents_);
			// Recurse to continue with further inheriting descendents
			linkInlawsOfAMethod_AndCausedByDescendentsOfAClass(origM, causeC);
		}
	}

	/**
	 Adds inlaw all records for this method.
	 Searches down its descendents and up their ancestors.
	
	 <p> Assumes the method has just been added to the model (e.g.,
	 the "add half" of a method rename, or a method move).
	 Otherwise, is a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>.
	
	 <p> Does nothing if this method cannot be inherited.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	
	 @see #linkInlawsCausedByThisClass
	 @see BT_Method#getInlaws()
	**/
	private static void linkInlawsOfAMethod(BT_Method am) {
		if (CHECK_JIKESBT && !am.getDeclaringClass().getRepository().factory.buildMethodRelationships)
			assertFailure("! am.getDeclaringClass().getRepository().factory.buildMethodRelationships");
		if (!am.canBeInherited())
			return;
		/*
		       Algorithm:
		       - Check kids classes.
		         - For each, check other parents for matches.
		         - Stop when hit a match.
		*/
		linkInlawsOfAMethod_AndCausedByDescendentsOfAClass(am, am.cls);
	}

//	/**
//	 Create records of inlaws where one is in or above topC, and the
//	 relationship is caused by a class that is at or below botC.
//	 This is used to focus the scope of the search for inlaws.
//	
//	 <p> This is _very_ similar to {@link BT_MethodRelationships#linkKidsOfAncestorMethodsThruAClass}.
//	 @param  topC  This and its ancestors contains methods whose inlaws are to be linked.
//	   An ancestor of botC.
//	 @param  exceptC  The class that _not_ to go up thru.
//	   Should be the parent of causeC that we one came down thru.
//	 @param  botC  A possible cause-class or the ancestor of some.
//	   I.e., this and its descendents are the only possible causing classes to be considered.
//	**/
//	static void linkInlawsAlongDescendentPath(
//		BT_Class topC,
//		BT_ClassVector seenCs,
//		BT_Class botC,
//		BT_Class exceptC) {
//		if (CHECK_JIKESBT_THOROUGHLY && !botC.isKidOf(exceptC))
//			assertFailure(Messages.getString("JikesBT.{0}_should_be_a_parent_of_{1}_51", new Object[] {exceptC, botC}));
//
//		perTopMethod : for (
//			int iTopM = 0;
//				iTopM < topC.methods.size();
//				++iTopM) { // Per unsure method in topC
//			BT_Method topM = topC.methods.elementAt(iTopM);
//			if (topM.canBeInherited()) { // It can have kids
//				// If there is a same-named method between topC and botC, don't try to add inlaws
//				for (int iSeenC = 0;
//					iSeenC < seenCs.size();
//					++iSeenC) { // Per seen class
//					BT_Class seenC = seenCs.elementAt(iSeenC);
//					if (seenC.findMethodOrNull(topM.name, topM.getSignature())
//						!= null)
//						continue perTopMethod; // topM has no kid via botC
//				} // Per seen class
//				linkInlawsOfAMethod_AndCausedByAClassOrDescendents(
//					topM,
//					botC,
//					exceptC);
//			} // It can have kids
//		} // Per unsure method in topC
//
//		seenCs.addElement(topC); // Push
//		BT_ClassVector pcs = topC.parents_;
//		for (int ipc = 0; ipc < pcs.size(); ++ipc) // Per parent of topC
//			linkInlawsAlongDescendentPath(
//				pcs.elementAt(ipc),
//				seenCs,
//				botC,
//				exceptC);
//		seenCs.removeElementAt(seenCs.size() - 1); // Pop
//	}

	/**
	 @see #delinkInlawsOfAllMethodsOfClassAndAncestors
	**/
	static void linkInlawsOfAllMethodsOfClassAndAncestors(BT_Class startC) {
		for (int im = 0;
			im < startC.methods.size();
			++im) { // Per parent class method
			linkInlawsOfAMethod(startC.methods.elementAt(im));
		}
		for (int ipc = 0;
			ipc < startC.parents_.size();
			++ipc) { // Per parent class
			BT_Class pc = startC.parents_.elementAt(ipc);
			linkInlawsOfAllMethodsOfClassAndAncestors(pc); // Recurse up
		}
	}

	/**
	 Updates the inlaw information for each method that this class implements or inherits.
	 Assumes that the methods' return types are compatible.
	 Assumes that all methods of all parents of this class have been loaded.
	 Assumes BT_Method.parents_ is correct for all related methods.
	 <p>
	 Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	
	 @see BT_Method#getInlaws()
	 @see #linkInlawsOfAMethod
	**/
	static void linkInlawsCausedByThisClass(BT_Class causeC) {
		if (CHECK_JIKESBT && !causeC.getRepository().factory.buildMethodRelationships)
			assertFailure("! causeC.getRepository().factory.buildMethodRelationships");
		
		if (causeC.parents_.size() >= 2) {
			// Are enough parents to cause inlaws
			BT_MethodVector[] inhMs =
				new BT_MethodVector[causeC.parents_.size()];
			// One set of methods per directly inherited/implemented class

			for (int ic = 0;
				ic < causeC.parents_.size();
				++ic) { // Per directly inherited (implemented or extended) class
				BT_Class c = causeC.parents_.elementAt(ic);
				inhMs[ic] = findAllMethodsWhichCanBeInheritedByAClass(c);
			}
			for (int ipc1 = 0;
				ipc1 < causeC.parents_.size();
				++ipc1) { // Per parent (directly inherited (implemented or extended)) class
				for (int iam = 0;
					iam < inhMs[ipc1].size();
					++iam) { // Per inherited or overridden/implemented method in the parent class
					BT_Method am1 = inhMs[ipc1].elementAt(iam);
					if (null
						== causeC.findMethodOrNull(am1.name, am1.getSignature())) {
						// Is an inherited ancestor method (it is not inside causeC)
						for (int ipc2 = ipc1 + 1;
							ipc2 < causeC.parents_.size();
							++ipc2) { // Per other, later parent (directly inherited/implemented) class
							BT_Method am2 =
								inhMs[ipc2].findMethod(am1.name, am1.getSignature());
							// Other ancestor method
							if (am2 != null) // Found such a method
								linkInlawPairIf(am1, am2, causeC);
						} // Per other, later parent (directly inherited/implemented) class
					} // Is an inherited ancestor method
				} // Per inherited or overridden/implemented method in the class
			} // Per parent (directly inherited (implemented or extended)) parent class
		} // Are enough parents to cause inlaws
	}

	// ----------------------------------------------------------------------------
	// :h- Inter-Method Relationship-Related -- Inlaws -- Relinking

	/**
	 Removes inlaws of the given method and fixes up its (to be
	 former) neighbors.
	 Is a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>
	 except that the inlaws of the method are deleted (and corresponding changes made).
	 Especially assumes its {@link BT_Method#getInlaws() inlaws} and
	 {@link BT_Method#getParents() parents} are still valid.
	**/
	static void relinkInlawsOfDeletedMethod(BT_Method deldM) {
		// if(trace_) traceln( "relinkInlawsOfDeletedMethod " + deldM);
		// My inlaws will become inlaws of my old (method-) parents
		for (int ii = deldM.inlaws.size() - 1;
			ii != -1;
			--ii) { // Per old inlaw backwards
			BT_MethodInlaw pair = deldM.inlaws.elementAt(ii);
			delinkInlawPairByIndex(deldM, ii);
			for (int ipm = 0;
				ipm < deldM.parents.size();
				++ipm) // Per old parent
				linkInlawPairIf(
					pair.getOtherMethod(deldM),
					deldM.parents.elementAt(ipm),
					pair.cls_);
		} // Per old inlaw backwards

		// Inlaws from my old ancestors down exactly to my old class and
		// immediately up elsewhere will be added (if I had >1 parent)
		for (int ipm1 = 0;
			ipm1 < deldM.parents.size();
			++ipm1) { // Per old parent
			BT_Method pm1 = deldM.parents.elementAt(ipm1);
			if (pm1.canBeInherited()) {
				// Old parent 1 can be inherited, so can have inlaws
				for (int ipm2 = 1 + ipm1;
					ipm2 < deldM.parents.size();
					++ipm2) { // Per later old parent
					BT_Method pm2 = deldM.parents.elementAt(ipm2);
					if (pm2.canBeInherited())
						// Old parent 2 can be inherited, so can have inlaws
						linkInlawPairIf(pm1, pm2, deldM.cls);
					// This class now causes them to be inlaws
				}
			}
		}
	}

	/**
	 Adds inlaws of the given method and fixes up its new neighbors.
	 Assumes its parents and kids are now valid.
	**/
	static void relinkInlawsOfAddedMethod(BT_Method addedM) {
		//         - Redo my new parents' (not ancestors) old inlaws (result should be only deleting some)
		//           - Scan & mark as new
		//           - Delete old ones
		//             - Cannot do better by moving them to thisC because don't know about inlaws that should have been _copied_,
		//               so have to redo my inlaws anyway
		//               - Actually, might be faster since wouldn't have to update collections as much, but not worth the bother yet.
		//         - Redo my inlaws
		//         - Branching==1 special case optimization:
		//           - If none of the descendents of my class had
		//             (==have) >1 parent, they didn't cause any inlaws,
		//             so none need be deleted, so nothing need be done.
		++BT_MethodInlaw.curMark_;
		// Mark each inlaw record as being validated now
		int curMark = BT_MethodInlaw.curMark_;

		// Algorithm reduces added & deleting from collections by marking valid ones, then deleting others.
		for (int ipm = 0;
			ipm < addedM.parents.size();
			++ipm) { // per parent (not ancestor) method
			BT_Method pm = addedM.parents.elementAt(ipm);
			linkInlawsOfAMethod(pm);
			for (int ii = pm.inlaws.size() - 1;
				ii != -1;
				--ii) { // Per BT_InlawPair
				BT_MethodInlaw pair = pm.inlaws.elementAt(ii);
				if (pair.tempMark_ != BT_MethodInlaw.curMark_) { // Old
					// if(trace_) traceln( "relinkInlawsOfAddedMethod delinking " + pm + " <-> " + pair.getOtherMethod(pm));
					delinkInlawPairByIndex(pm, ii);
				} // Old
			} // Per BT_InlawPair
		} // per parent (not ancestor) method

		if (CHECK_JIKESBT && curMark != BT_MethodInlaw.curMark_)
			assertFailure(Messages.getString("JikesBT.Another_agent_changed_curMark__57"));

		linkInlawsOfAMethod(addedM);
	}
	// ----------------------------------------------------------------------------
	// :h- The catch-all (actually, the catch-some)

	/**
	 Implements {@link BT_MethodVector#refreshInterMethodRelationships}.
	 Must not be called unless {@link BT_Factory#buildMethodRelationships buildMethodRelationships} is true.
	
	**/
//	static void refreshInterMethodRelationships(BT_MethodVector ms) {
//
//		// First, detach them all.
//		for (int im = 0; im < ms.size(); ++im) {
//			BT_Method m = ms.elementAt(im);
//			delinkParents(m);
//			delinkKids(m);
//			delinkInlawsOfMethod(m);
//		}
//
//		// Then attach them all.
//		for (int im = 0; im < ms.size(); ++im) {
//			BT_Method m = ms.elementAt(im);
//			if (m.canInherit())
//				linkParents(m);
//			if (m.canBeInherited()) {
//				linkKids(m);
//				linkInlawsOfAMethod(m);
//			}
//		}
//	}
}

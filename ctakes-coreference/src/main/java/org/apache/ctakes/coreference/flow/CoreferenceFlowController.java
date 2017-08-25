/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ctakes.coreference.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference.EndDocsSentinelAnnotator;
import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference.NewDocSentinelAnnotator;
import org.apache.ctakes.temporal.utils.PatientViewsUtil;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.cas.CASException;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/* This flow control section borrows from the UIMA implementation of FixedFlowController
 * and its internal Flow object. Simple change to check if there are any gold
 * coref annotations inside the cas, and if not skip out so we don't waste
 * time running coref code on those (since we're not going to print out the answers
 * anyways)
 */
public class CoreferenceFlowController extends org.apache.uima.flow.JCasFlowController_ImplBase {
  private List<String> mSequence;
  private HashMap<String,Integer> name2Docnum = new HashMap<>();
  private static Pattern dupPatt = Pattern.compile("^(\\S+)\\.(\\d+)$");
  
  @Override
  public void initialize(FlowControllerContext context)
      throws ResourceInitializationException {
    super.initialize(context);
    
    FlowConstraints flowConstraints = context.getAggregateMetadata().getFlowConstraints();
    int docNum = 0;
    mSequence = new ArrayList<>();
    if (flowConstraints instanceof FixedFlow) {
      String[] sequence = ((FixedFlow) flowConstraints).getFixedFlow();
      for(String annotatorName : sequence){
        if(annotatorName.startsWith(NewDocSentinelAnnotator.class.getName())){
          docNum++;
        }else if(annotatorName.startsWith(EndDocsSentinelAnnotator.class.getName())){
          // we are now in annotators that aren't repeats (they operate over the whole CAS, not a single document view),
          // but we have seen repeats before. That means we are after the repeating section. Set document number back to
          // 0 and reset firstRepeat to null
          docNum = 0;
        }
        name2Docnum.put(annotatorName, docNum);
      }
      mSequence.addAll(Arrays.asList(sequence));
    } else {
      throw new ResourceInitializationException(ResourceInitializationException.FLOW_CONTROLLER_REQUIRES_FLOW_CONSTRAINTS,
              new Object[]{this.getClass().getName(), "fixedFlow", context.getAggregateMetadata().getSourceUrlString()});
    }
  }

  @Override
  public Flow computeFlow(JCas jcas) throws AnalysisEngineProcessException {
    return new CorefEvalFlow(jcas, 0);
  }
  
  class CorefEvalFlow extends JCasFlow_ImplBase {

    private JCas jcas;
    private int currentStepInd;
    private int numDocs = -1;
    
    public CorefEvalFlow(JCas jcas, int step){
      this.jcas = jcas;
      this.currentStepInd = step;
    }

    @Override
    public Step next() {
      // First find the view that tells us how many documents we have for this patient.
      if(this.numDocs < 0){          
        try{
          Iterator<JCas> viewIter = jcas.getViewIterator();
          while(viewIter.hasNext()){
            JCas view = viewIter.next();
            if(view.getViewName().equals(PatientViewsUtil.getNumDocsViewName())){
              JCas numDocsView = jcas.getView(PatientViewsUtil.getNumDocsViewName());
              numDocs = Integer.parseInt(numDocsView.getDocumentText());
            }
          }
        }catch(CASException e){
          throw new RuntimeException(e);
        }
      }

      // if we are past the last annotator finish
      if (currentStepInd >= mSequence.size()) {
        return new FinalStep();
      }

      String currentStep = mSequence.get(currentStepInd);
      // if we know how many documents there are start looking for 
      if(this.numDocs >= 0){
        int docNum = name2Docnum.get(currentStep);
        while(docNum > this.numDocs){
          if(currentStepInd+1 >= mSequence.size()){
            return new FinalStep();
          }
          String nextStep = mSequence.get(++currentStepInd);
          docNum = name2Docnum.get(nextStep);
        }
      }
            
      // otherwise finish
      return new SimpleStep(mSequence.get(currentStepInd++));
    }
  }
}
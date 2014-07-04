package org.cleartk.ml.tksvmlight.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.NotImplementedException;
import org.cleartk.ml.tksvmlight.TreeFeatureVector;
import org.cleartk.util.treebank.TopTreebankNode;
import org.cleartk.util.treebank.TreebankFormatParser;
import org.cleartk.util.treebank.TreebankNode;


public class SubsetTreeKernel implements TreeKernel {
  public static final double LAMBDA_DEFAULT = 0.4;
  private double lambda = LAMBDA_DEFAULT;

  private boolean normalize = false;
  
  private ConcurrentHashMap<String, Double> normalizers = new ConcurrentHashMap<String, Double>();

  private ForestSumMethod sumMethod = ForestSumMethod.SEQUENTIAL;
  HashMap<String, TopTreebankNode> trees = null;

  public SubsetTreeKernel(
      double lambda,
      ForestSumMethod sumMethod,
      boolean normalize) {
    this.lambda = lambda;
    this.sumMethod = sumMethod;
    this.normalize = normalize;
    trees = new HashMap<String, TopTreebankNode>();
  }

  @Override
  public double evaluate(TreeFeatureVector fv1, TreeFeatureVector fv2) {
    double sim = 0.0;
    if (sumMethod == ForestSumMethod.SEQUENTIAL) {
      List<String> fv1Trees = new ArrayList<String>(fv1.getTrees().values());
      List<String> fv2Trees = new ArrayList<String>(fv2.getTrees().values());
      for (int i = 0; i < fv1Trees.size(); i++) {
        String tree1Str = fv1Trees.get(i);
        String tree2Str = fv2Trees.get(i);
        sim += sst(tree1Str, tree2Str);
      }
    } else {
      throw new NotImplementedException("The only summation method implemented is Sequential!");
    }
    return sim;
  }

  private double sst(String tree1Str, String tree2Str) {

    TopTreebankNode node1 = null;
    if (!trees.containsKey(tree1Str)) {
      node1 = TreebankFormatParser.parse(tree1Str);
      trees.put(tree1Str, node1);
    } else
      node1 = trees.get(tree1Str);

    TopTreebankNode node2 = null;
    if (!trees.containsKey(tree2Str)) {
      node2 = TreebankFormatParser.parse(tree2Str);
      trees.put(tree2Str, node2);
    } else
      node2 = trees.get(tree2Str);

    double norm1 = 0.0;
    double norm2 = 0.0;
    if (normalize) {
      if (!normalizers.containsKey(tree1Str)) {
        double norm = sim(node1, node1);
        normalizers.put(tree1Str, norm);
      }
      if (!normalizers.containsKey(tree2Str)) {
        double norm = sim(node2, node2);
        normalizers.put(tree2Str, norm);
      }

      norm1 = normalizers.get(tree1Str);
      norm2 = normalizers.get(tree2Str);
    }
    if (normalize) {
      return (sim(node1, node2) / Math.sqrt(norm1 * norm2));
    } else {
      return sim(node1, node2);
    }
  }

  private double sim(TreebankNode node1, TreebankNode node2) {
    double sim = 0.0;
    List<TreebankNode> N1 = TreeKernelUtils.getNodeList(node1);
    List<TreebankNode> N2 = TreeKernelUtils.getNodeList(node2);
    for (TreebankNode n1 : N1) {
      for (TreebankNode n2 : N2) {
        sim += numCommonSubtrees(n1, n2);
      }
    }
    return sim;
  }

  private double numCommonSubtrees(TreebankNode n1, TreebankNode n2) {
    double retVal = 1.0;
    List<TreebankNode> children1 = n1.getChildren();
    List<TreebankNode> children2 = n2.getChildren();
    int c1size = children1.size();
    int c2size = children2.size();
    if (c1size != c2size) {
      retVal = 0;
    } else if (!n1.getType().equals(n2.getType())) {
      retVal = 0;
    } else if (n1.isLeaf() && n2.isLeaf()) {
      // both are preterminals, and we know they have the same type, need to check value (word)
      // Collins & Duffy tech report says lambdaSquared, but Nips 02 paper uses lambda
      // Moschitti's papers also use lambda
      // retVal = lambdaSquared;
      if (n1.getValue().equals(n2.getValue())) {
        retVal = lambda;
      } else {
        retVal = 0;
      }
    } else {
      // At this point they have the same label and same # children. Check if children the same.
      boolean sameProd = true;
      for (int i = 0; i < c1size; i++) {
        String l1 = children1.get(i).getType();
        String l2 = children2.get(i).getType();
        if (!l1.equals(l2)) {
          sameProd = false;
          break;
        }
      }
      if (sameProd == true) {
        for (int i = 0; i < c1size; i++) {
          retVal *= (1 + numCommonSubtrees(children1.get(i), children2.get(i)));
        }
        // again, some disagreement in the literature, with Collins and Duffy saying
        // lambdaSquared here in tech report and lambda here in nips 02. We'll stick with
        // lambda b/c that's what moschitti's code (which was presumably used for model-building)
        // uses.
        retVal = lambda * retVal;
      } else {
        retVal = 0;
      }
    }
    return retVal;
  }

}

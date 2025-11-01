/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.id3_kpdl;

/**
 *
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author nguye
 */
public class ID3_KPDL {

    static class Node {

        String attribute;
        String label;
        Map<String, Node> children;

        Node(String attribute) {
            this.attribute = attribute;
            this.children = new HashMap<>();
        }

        Node(String attribute, String label) {
            this.attribute = attribute;
            this.label = label;
        }

        public Node() {
            this.children = new HashMap<>();
        }
    }

    private static List<Row> trainData; // Lưu tập train để sử dụng trong predict

    // Hàm tính entropy
    public static double calculateEntropy(List<Row> data, int classCol) {
        Map<String, Integer> classCounts = new HashMap<>();
        int total = 0;
        for (Row row : data) {
            Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String label = cell.toString().trim();
            if (label.isEmpty()) {
                continue;
            }
            classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
            total++;
        }
        double entropy = 0.0;
        for (int count : classCounts.values()) {
            double prob = (double) count / total;
            entropy -= prob * Math.log(prob) / Math.log(2);
        }
        return entropy;
    }

    // Hàm tính Information Gain
    public static double calculateInformationGain(List<Row> data, int attrCol, int classCol) {
        double totalEntropy = calculateEntropy(data, classCol);
        Map<String, List<Row>> subsets = new HashMap<>();
        int totalSize = 0;
        for (Row row : data) {
            Cell attrCell = row.getCell(attrCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (attrCell.getCellType() == CellType.BLANK) {
                continue;
            }
            String attrValue = attrCell.toString().trim();
            if (attrValue.isEmpty()) {
                continue;
            }
            subsets.computeIfAbsent(attrValue, k -> new ArrayList<>()).add(row);
            totalSize++;
        }
        double subsetEntropy = 0.0;
        for (List<Row> subset : subsets.values()) {
            double prob = (double) subset.size() / totalSize;
            subsetEntropy += prob * calculateEntropy(subset, classCol);
        }
        return totalEntropy - subsetEntropy;
    }

    // Hàm tìm thuộc tính tốt nhất
    public static int findBestAttribute(List<Row> data, List<Integer> availableAttrs, int classCol) {
        double maxGain = -1;
        int bestAttr = -1;

        for (int attr : availableAttrs) {
            double gain = calculateInformationGain(data, attr, classCol);
            if (gain > maxGain) {
                maxGain = gain;
                bestAttr = attr;
            }
        }
        return bestAttr;
    }

    // Hàm lay nhãn duy nhất
    public static String getUniqueLabel(List<Row> data, int classCol) {
        String firstLabel = null;
        for (Row row : data) {
            Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String label = cell.toString().trim();
            if (label.isEmpty()) {
                continue;
            }

            if (firstLabel == null) {
                firstLabel = label;
            } else if (!label.equals(firstLabel)) {
                return null;
            }
        }
        return firstLabel;
    }

    // Hàm lấy nhãn phổ biến nhất
    public static String getMajorityLabel(List<Row> data, int classCol) {
        Map<String, Integer> classCounts = new HashMap<>();
        for (Row row : data) {
            Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String label = cell.toString().trim();
            if (label.isEmpty()) {
                continue;
            }
            classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
        }

        String majorityLabel = null;
        int maxCount = -1;
        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                majorityLabel = entry.getKey();
            }
        }
        return majorityLabel;
    }

    // Thuật toán ID3
    public static Node id3(List<Row> D, List<Integer> A, int C) {
        //Buoc 1: Tao node goc
        Node node = new Node();
        //Buoc 2: Kiem tra TH dac biet
        // Trường hợp 1: Tất cả mẫu có cùng lop
        String uniqueLabel = getUniqueLabel(D, C);
        if (uniqueLabel != null) {
            return new Node(null, uniqueLabel);
        }

        // Trường hợp 2: A rong
        if (A.isEmpty()) {
            String majorityLabel = getMajorityLabel(D, C);
            return new Node(null, majorityLabel);
        }
        //Buoc 3:
        // Tìm thuộc tính tốt nhất X, gan nhan nut goc voi thuoc tinh X
        int X = findBestAttribute(D, A, C);
        if (X == -1) {
            String majorityLabel = getMajorityLabel(D, C);
            return new Node(null, majorityLabel);
        }
        String[] attrNames = {"pH", "Hardness", "Solids", "Chloramine", "Sulfate", "Conductive", "Organic Carbon", "Trihalomethanes", "Turbidity"};
        node.attribute = attrNames[X];
        // Loại bỏ thuộc tính đã chọn
        List<Integer> newAttrs = new ArrayList<>(A);
        newAttrs.remove(Integer.valueOf(X));
        Map<String, List<Row>> new_D = new HashMap<>();
        // For each giatri v cua thuoc tinh X
        for (Row row : D) {
            new_D.computeIfAbsent(row.getCell(X).toString(), k -> new ArrayList<>()).add(row);
        }
        // Đệ quy cho từng tập con
        for (Map.Entry<String, List<Row>> entry : new_D.entrySet()) {
            String attrValue = entry.getKey();
            List<Row> Dv = entry.getValue();
            //If Dv rong
            if (Dv.isEmpty()) {
                String majorityLabel = getMajorityLabel(D, C);
                node.children.put(attrValue, new Node(null, majorityLabel));
            } else {
                //Dv khong rong goi de quy
                node.children.put(attrValue, id3(Dv, newAttrs, C));
            }
        }
        return node;
    }

    // Hàm in cây
    public static void printTree(Node node, String indent) {
        if (node.label != null) {
            System.out.println(indent + "Leaf: " + node.label);
            return;
        }
        System.out.println(indent + "Node: " + node.attribute);
        for (Map.Entry<String, Node> entry : node.children.entrySet()) {
            System.out.println(indent + "  -> " + entry.getKey());
            printTree(entry.getValue(), indent + "    ");
        }
    }

    // Hàm dự đoán
    public static String predict(Node tree, Row row) {
        Node current = tree;
        String[] attrNames = {"pH", "Hardness", "Solids", "Chloramine", "Sulfate", "Conductive", "Organic Carbon", "Trihalomethanes", "Turbidity"};
        while (current.label == null) {
            String attr = current.attribute;
            int attrIndex = -1;
            for (int i = 0; i < attrNames.length; i++) {
                if (attrNames[i].equals(attr)) {
                    attrIndex = i;
                    break;
                }
            }
            Cell cell = row.getCell(attrIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK) {
                return getMajorityLabel(trainData, row.getLastCellNum() - 1);
            }
            String attrValue = cell.toString().trim();
            if (attrValue.isEmpty()) {
                return getMajorityLabel(trainData, row.getLastCellNum() - 1);
            }
            current = current.children.get(attrValue);
            if (current == null) {
                return getMajorityLabel(trainData, row.getLastCellNum() - 1);
            }
        }
        return current.label;
    }

    public static void splitData(List<Row> data, List<Row> train, List<Row> test, double trainRatio) {
        Collections.shuffle(data, new Random());
        int trainSize = (int) (data.size() * trainRatio);
        train.addAll(data.subList(0, trainSize));
        test.addAll(data.subList(trainSize, data.size()));
    }

    public static void evaluateModel(Node tree, List<Row> testData, int classCol) {
        int truePositives = 0, falsePositives = 0, falseNegatives = 0, trueNegatives = 0;
        int correct = 0;
        int total = 0;

        // Xác định nhãn positive và negative DỰA TRÊN HIỂU BIẾT DỮ LIỆU CỦA BẠN
        String positiveLabel = "1.0"; // **ĐIỀN NHÃN POSITIVE THỰC TẾ**
        String negativeLabel = "0.0"; // **ĐIỀN NHÃN NEGATIVE THỰC TẾ**
        System.out.println("Positive label: " + positiveLabel + ", Negative label: " + negativeLabel);

        // Kiểm tra nhãn thực tế trong tập kiểm thử
        Map<String, Integer> actualLabelCounts = new HashMap<>();
        for (Row row : testData) {
            Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String label = cell.toString().trim();
            if (label.isEmpty()) {
                continue;
            }
            actualLabelCounts.put(label, actualLabelCounts.getOrDefault(label, 0) + 1);
        }
        System.out.println("Actual labels in test data: " + actualLabelCounts);

        // Đánh giá từng mẫu
        for (Row row : testData) {
            Cell actualCell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (actualCell.getCellType() == CellType.BLANK) {
                continue;
            }
            String actualLabel = actualCell.toString().trim();
            if (actualLabel.isEmpty()) {
                continue;
            }
            String predictedLabel = predict(tree, row);
            total++;

            System.out.println("Row " + row.getRowNum() + ": Actual=" + actualLabel + ", Predicted=" + predictedLabel);

            if (predictedLabel.equals(actualLabel)) {
                correct++;
                if (actualLabel.equals(positiveLabel)) {
                    truePositives++;
                } else if (actualLabel.equals(negativeLabel)) {
                    trueNegatives++;
                }
            } else {
                if (predictedLabel.equals(positiveLabel)) {
                    falsePositives++;
                } else if (predictedLabel.equals(negativeLabel)) {
                    falseNegatives++;
                }
            }
        }

        // In số liệu để debug
        System.out.println("True Positives: " + truePositives);
        System.out.println("False Positives: " + falsePositives);
        System.out.println("False Negatives: " + falseNegatives);
        System.out.println("True Negatives: " + trueNegatives);

        // Tính các chỉ số
        double accuracy = total > 0 ? (double) correct / total : 0;
        double precision = truePositives + falsePositives > 0 ? (double) truePositives / (truePositives + falsePositives) : 0;
        double recall = truePositives + falseNegatives > 0 ? (double) truePositives / (truePositives + falseNegatives) : 0;
        double f1Score = precision + recall > 0 ? 2 * (precision * recall) / (precision + recall) : 0;

        System.out.println("\nModel Evaluation on Test Set:");
        System.out.printf("Accuracy: %.4f\n", accuracy);
        System.out.printf("Precision: %.4f\n", precision);
        System.out.printf("Recall: %.4f\n", recall);
        System.out.printf("F1-Score: %.4f\n", f1Score);
    }

    public static void splitDataFixed(List<Row> data, List<Row> trainData, List<Row> testData, double trainRatio) {
        int totalSize = data.size();
        int trainSize = (int) (totalSize * trainRatio);

        for (int i = 0; i < trainSize; i++) {
            trainData.add(data.get(i));
        }

        for (int i = trainSize; i < totalSize; i++) {
            testData.add(data.get(i));
        }
    }

    public static void main(String[] args) {
        try {
            File file = new File("D:\\ID3_KPDL\\src\\main\\java\\com\\mycompany\\id3_kpdl\\water_quality.xlsx");
            if (!file.exists()) {
                System.err.println("File not found: " + file.getPath());
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            // In tiêu đề để kiểm tras
            Row headerRow = sheet.getRow(0);
            System.out.println("Header: ");
            for (Cell cell : headerRow) {
                System.out.print(cell.toString() + "\t");
            }
            System.out.println();

            // In phân phối nhãn trước khi chuẩn hóa
            Map<String, Integer> labelCounts = new HashMap<>();
            int classCol = 9;
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (cell.getCellType() == CellType.BLANK) {
                    continue;
                }
                String label = cell.toString().trim();
                labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
            }
            System.out.println("Label distribution before preprocessing: " + labelCounts);

            // In phân phối nhãn sau khi chuẩn hóa
            labelCounts.clear();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Cell cell = row.getCell(classCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (cell.getCellType() == CellType.BLANK) {
                    continue;
                }
                String label = cell.toString().trim();
                labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
            }
            System.out.println("Label distribution after preprocessing: " + labelCounts);

            List<Row> data = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    data.add(row);
                }
            }

            // Chia dữ liệu 70% train, 30% test
            trainData = new ArrayList<>();
            List<Row> testData = new ArrayList<>();
            splitDataFixed(data, trainData, testData, 0.8);
            System.out.println("\nTrain size: " + trainData.size());
            System.out.println("Test size: " + testData.size());

            // Danh sách thuộc tính
            List<Integer> availableAttrs = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                availableAttrs.add(i);
            }

            // Cột nhãn lớp (Potability, cột 9)
            classCol = 9;

            // Huấn luyện cây ID3 trên tập train
            Node decisionTree = id3(trainData, availableAttrs, classCol);

            // In cây
            System.out.println("\nDecision Tree:");
            printTree(decisionTree, "");

            // Đánh giá mô hình trên tập test
            evaluateModel(decisionTree, testData, classCol);

            fis.close();
            workbook.close();
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

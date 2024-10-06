package com.mycompany.backendassignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PortfolioCalculator {

    static class Transaction {
        public String trxnDate;
        public String scheme;
        public String schemeName;
        public String purchasePrice;
        public String trxnUnits;
        public String trxnAmount;
        public String folio;
        public String broker;
        public String isin;
        // Getters and setters (optional)
    }

    // Method to get the current NAV (for simplicity, hardcoded)
    public static double getCurrentNav(String isin) {
        // This is where you'd call mstarpy or a similar service to get current NAV
        // For now, we'll return a mock NAV value
        return 100.0; // Assume a mock NAV of 100 for all schemes
    }

    // Method to process transactions and calculate portfolio value and gain
    public static Map<String, Map<String, Double>> processTransactions(List<Transaction> transactions) {
        Map<String, Map<String, Double>> portfolio = new HashMap<>();

        for (Transaction transaction : transactions) {
            String key = transaction.schemeName + "-" + transaction.folio;
            double trxnUnits = Double.parseDouble(transaction.trxnUnits);
            double purchasePrice = Double.parseDouble(transaction.purchasePrice);

            portfolio.putIfAbsent(key, new HashMap<>());
            Map<String, Double> data = portfolio.get(key);
            double units = data.getOrDefault("units", 0.0);
            double purchaseCost = data.getOrDefault("purchaseCost", 0.0);

            if (trxnUnits > 0) {
                // Buying units
                units += trxnUnits;
                purchaseCost += trxnUnits * purchasePrice;
            } else {
                // Selling units (FIFO logic)
                double sellUnits = Math.abs(trxnUnits);
                if (sellUnits <= units) {
                    units -= sellUnits;
                    purchaseCost -= sellUnits * purchasePrice;
                }
            }

            data.put("units", units);
            data.put("purchaseCost", purchaseCost);
        }

        return portfolio;
    }

    // Method to calculate the total portfolio value and gain
    public static void calculatePortfolioValueAndGain(List<Transaction> transactions) {
        Map<String, Map<String, Double>> portfolio = processTransactions(transactions);
        double totalPortfolioValue = 0.0;
        double totalPortfolioGain = 0.0;

        for (Map.Entry<String, Map<String, Double>> entry : portfolio.entrySet()) {
            String key = entry.getKey();
            String isin = transactions.get(0).isin; // Assuming all have the same ISIN
            double currentNav = getCurrentNav(isin);
            Map<String, Double> data = entry.getValue();
            double units = data.get("units");
            double purchaseCost = data.get("purchaseCost");

            double currentValue = units * currentNav;
            double gain = currentValue - purchaseCost;
            totalPortfolioValue += currentValue;
            totalPortfolioGain += gain;
        }

        System.out.println("Total Portfolio Value: " + totalPortfolioValue);
        System.out.println("Total Portfolio Gain: " + totalPortfolioGain);
    }

    // Method to calculate XIRR using the BrentSolver
    public static double calculateXIRR(List<Transaction> transactions, double currentPortfolioValue) {
        List<Double> cashFlows = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            for (Transaction transaction : transactions) {
                cashFlows.add(-Double.parseDouble(transaction.trxnAmount)); // Negative cash outflow
                dates.add(sdf.parse(transaction.trxnDate));
            }

            // Add the current portfolio value as the last cash flow
            cashFlows.add(currentPortfolioValue);
            dates.add(new Date()); // Today’s date
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return calculateXIRRInternal(cashFlows, dates);
    }

    // XIRR Internal Calculation
    public static double calculateXIRRInternal(List<Double> cashFlows, List<Date> dates) {
        double xirr = new BrentSolver().solve(100, new XirrFunction(cashFlows, dates), -0.9999999, 1.0);
        return xirr;
    }

    // Define XIRR function using the UnivariateFunction interface
    static class XirrFunction implements UnivariateFunction {
        private final List<Double> cashFlows;
        private final List<Date> dates;

        public XirrFunction(List<Double> cashFlows, List<Date> dates) {
            this.cashFlows = cashFlows;
            this.dates = dates;
        }

        @Override
        public double value(double rate) {
            double result = 0.0;
            
            if (cashFlows != null && cashFlows.size() > 0) {
              Date startDate = dates.get(0);
              for (int i = 0; i < cashFlows.size(); i++) {
                double cashFlow = cashFlows.get(i);
                Date date = dates.get(i);
                double years = (date.getTime() - startDate.getTime()) / (365.25 * 24 * 60 * 60 * 1000.0);
                result += cashFlow / Math.pow(1.0 + rate, years);
            }
} else {
   
    System.out.println("No transactions available to process.");
}
          return result;
        }
    }

    public static void main(String[] args) {
        // Load transaction data from JSON file
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> data = mapper.readValue(new File("C:/Users/Harsh Sharma/OneDrive/Documents/NetBeansProjects/BackendAssignment/transaction_data.json"), new TypeReference<Map<String, Object>>() {});
            List<Transaction> transactions = mapper.convertValue(data.get("DTtransaction"), new TypeReference<List<Transaction>>() {});

            // Calculate Portfolio Value and Gain
            calculatePortfolioValueAndGain(transactions);

            // Calculate XIRR (optional)
            double currentPortfolioValue = 10000.0; // Example current portfolio value
            double xirr = calculateXIRR(transactions, currentPortfolioValue);
            System.out.println("Portfolio XIRR: " + (xirr * 100) + "%");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

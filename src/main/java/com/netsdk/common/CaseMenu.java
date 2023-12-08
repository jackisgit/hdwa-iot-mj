package com.netsdk.common;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

@Slf4j
public class CaseMenu {

    private Vector<Item> items;

    public CaseMenu() {
        super();
        items = new Vector<Item>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    private void showItem() {
        final String format = "%2d\t%-20s\n";
        int index = 0;
        log.info(format, index++, "exit App");
        for (Item item : items) {
            log.info(format, index++, item.getItemName());
        }
        log.info("Please input a item index to invoke the method:");
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            showItem();
            try {
                int input = Integer.parseInt(scanner.nextLine());

                if (input <= 0) {
                    log.error("input <= 0 || scanner.nextLine() == null");
//					scanner.close();
//					System.exit(0);
                    break;
                }

                if (input < 0 || input > items.size()) {
                    log.error("Input Error Item Index.");
                    continue;
                }

                Item item = items.get(input - 1);
                Class<?> itemClass = item.getObject().getClass();
                Method method = itemClass.getMethod(item.getMethodName());
                method.invoke(item.getObject());
            } catch (NoSuchElementException e) {
//				scanner.close();
//				System.exit(0);
                break;
            } catch (NumberFormatException e) {
                log.error("Input Error NumberFormat.");
                continue;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        scanner.close();
    }

    public static class Item {
        private Object object;
        private String itemName;
        private String methodName;

        public Item(Object object, String itemName, String methodName) {
            super();
            this.object = object;
            this.itemName = itemName;
            this.methodName = methodName;
        }

        public Object getObject() {
            return object;
        }

        public String getItemName() {
            return itemName;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}

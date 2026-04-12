public class TestDuplicatesJava {
    public void method1() {
        System.out.println("Hello");
        System.out.println("World");
    }

    public void method2() {
        System.out.println("Hello");
        System.out.println("World");
    }

    public void method3() {
        System.out.println("Hello");
        System.out.println("World");
    }

    public void complexMethod() {
        int x = 1;
        int y = 2;
        int z = 3;
        if (x > 0) {
            if (y > 0) {
                if (z > 0) {
                    System.out.println("All positive");
                }
            }
        }
    }

    public void securityMethod() {
        String name = null;
        System.out.println(name); // Potential NPE, but not !!
    }

    public void performanceMethod() {
        try {
            Thread.sleep(1000); // Blocking call
        } catch (Exception e) { // Broad exception
            System.out.println("Error");
        }
    }
}
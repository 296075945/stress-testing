package com.wy.stress.testing;

import java.util.concurrent.ThreadLocalRandom;

public class Hello {

    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient(1000, "localhost", 8080);

        for (int i = 0; i < 10000; i++) {

            String url = "http://localhost:8080/test2?id=" + ThreadLocalRandom.current().nextInt(30);

            try {

                client.post(url, null, null);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

    }
}

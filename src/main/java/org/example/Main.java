package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.helper.PropertyHolder;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.EnumSet;
import java.util.Properties;

import static java.lang.System.*;

/**
 * это мейн
 *
 * @author Михаил Александров
 * @since 18.05.2024
 */
public class Main {
    public static void main(String[] args) throws IOException {
        new Bot().run();
    }
}
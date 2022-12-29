import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

class WatermarkRemover extends PDFStreamEngine {
    private ContentStreamWriter writer;
    private PDStream stream;

    public void remove(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            stream = new PDStream(document);
            OutputStream os = stream.createOutputStream(COSName.FLATE_DECODE);
            writer = new ContentStreamWriter(os);
            processPage(page);
            os.flush();
            os.close();
            page.setContents(stream);
            page.getResources().getCOSObject().removeItem(COSName.PATTERN);
        }
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        if (operator.getName().equals(OperatorName.SHOW_TEXT_LINE)) {
            COSString str = (COSString) operands.get(0);
            if (str.getString().contains("Evaluation")) {
                return;
            }
        }
        writer.writeTokens(operands);
        writer.writeToken(operator);
        super.processOperator(operator, operands);
    }
}

public class Main {
    static void runGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("去除水印");
                frame.setLayout(new GridLayout(2, 2));
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel input = new JLabel("");
                frame.getContentPane().add(input);
                JButton button = new JButton("选择文件");
                frame.getContentPane().add(button);
                JLabel output = new JLabel("");
                frame.getContentPane().add(output);
                JButton remove = new JButton("去除水印");
                frame.getContentPane().add(remove);
                button.addActionListener(e -> {
                    input.setText("");
                    output.setText("");
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileNameExtensionFilter("pdf", "pdf"));
                    int ret = chooser.showOpenDialog(frame);
                    if (ret == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        String name = file.getAbsolutePath();
                        name = name.substring(0, name.length() - 4);
                        name += "_removed.pdf";
                        input.setText(file.getAbsolutePath());
                        output.setText(name);
                        frame.pack();
                    }
                });
                remove.addActionListener(e -> {
                    try {
                        Main.remove(input.getText(), output.getText());
                        JOptionPane.showConfirmDialog(frame, "去除成功", "提示", JOptionPane.YES_NO_OPTION);
                    } catch (Exception ee) {
                        JOptionPane.showConfirmDialog(frame, "去除失败：" + ee, "提示", JOptionPane.YES_NO_OPTION);
                    }
                });
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        runGUI();
    }

    static void remove(String input, String output) {
        try {
            PDDocument document = PDDocument.load(new File(input));
            WatermarkRemover remover = new WatermarkRemover();
            remover.remove(document);
            document.save(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
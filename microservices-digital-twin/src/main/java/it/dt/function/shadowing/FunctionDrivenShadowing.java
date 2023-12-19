package it.dt.function.shadowing;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import it.dt.function.augmentation.CommandLineResult;
import it.dt.function.augmentation.PythonCliExecutor;
import it.dt.function.config.FunctionConfiguration;
import it.wldt.adapter.digital.event.DigitalActionWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;
import it.wldt.core.model.ShadowingModelFunction;
import it.wldt.core.state.DigitalTwinStateProperty;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 17/11/2023 - 14:42
 */
public class FunctionDrivenShadowing extends ShadowingModelFunction {

    private final static Logger logger = LoggerFactory.getLogger(FunctionDrivenShadowing.class);

    private MetricRegistry metricRegistry = null;

    public FunctionDrivenShadowing() {
        super("FunctionDrivenShadowing");

        this.metricRegistry = new MetricRegistry();

        //Create Metrics CSV Reporter
        final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(new File("metrics/"));

        reporter.start(1, TimeUnit.SECONDS);
    }

    @Override
    protected void onCreate() {
        logger.info("FunctionDrivenShadowing onCreate()");
    }

    @Override
    protected void onStart() {
        logger.info("FunctionDrivenShadowing onStart()");
    }

    @Override
    protected void onStop() {
        logger.info("FunctionDrivenShadowing onStop()");
    }

    @Override
    protected void onDigitalTwinBound(Map<String, PhysicalAssetDescription> adaptersPhysicalAssetDescriptionMap) {

        logger.info("FunctionDrivenShadowing onDigitalTwinBound()");

        try{
            adaptersPhysicalAssetDescriptionMap.values().forEach(pad -> {
                pad.getProperties().forEach(property -> {
                    try {
                        //Create and write the property on the DT's State
                        this.digitalTwinState.createProperty(new DigitalTwinStateProperty<>(property.getKey(),(String) property.getInitialValue()));
                        this.observePhysicalAssetProperty(property);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            });

            //Create RESULT Property Field
            this.digitalTwinState.createProperty(new DigitalTwinStateProperty<>(FunctionConfiguration.DT_RESULT_PROPERTY, ""));

            //Start observation to receive all incoming Digital Action through active Digital Adapter
            //Without this call the Shadowing Function will not receive any notifications or callback about
            //incoming request to execute an exposed DT's Action
            observeDigitalActionEvents();

            //Notify the DT Core that the Bounding phase has been correctly completed and the DT has evaluated its
            //internal status according to what is available and declared through the Physical Adapters
            notifyShadowingSync();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String s) {
        logger.info("FunctionDrivenShadowing onDigitalTwinUnBound()");
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String s, PhysicalAssetDescription physicalAssetDescription) {
        logger.info("FunctionDrivenShadowing onPhysicalAdapterBidingUpdate()");
    }

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> physicalAssetPropertyWldtEvent) {

        final Timer timer = metricRegistry.timer(name(FunctionDrivenShadowing.class, "function-execution"));
        final Timer.Context context = timer.time();

        try{

            if(physicalAssetPropertyWldtEvent.getBody() instanceof  String){

                String imagePayloadBase64String = (String) physicalAssetPropertyWldtEvent.getBody();

                //Save Image
                saveReceivedImage(imagePayloadBase64String);

                //Execute Python Script
                String executionResult = executePythonFunction();

                //Publish Result
                if(executionResult != null)
                    this.digitalTwinState.updateProperty(new DigitalTwinStateProperty<>(FunctionConfiguration.DT_RESULT_PROPERTY, executionResult));
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            context.stop();
        }
    }

    private void saveReceivedImage(String imagePayloadBase64String){
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(imagePayloadBase64String);
            String outputFileName = "data/dt_shadowing_image.png";
            FileUtils.writeByteArrayToFile(new File(outputFileName), decodedBytes);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String executePythonFunction(){
        try{

            PythonCliExecutor pythonCliExecutor = new PythonCliExecutor("python3");
            CommandLineResult commandLineResult = pythonCliExecutor.executePythonCommand("functions/target_shadowing_function.py", new ArrayList<String>());

            if(commandLineResult.getExitCode() >= 0)
                return commandLineResult.getOutputLog();
            else
                return null;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPhysicalAssetEventNotification(PhysicalAssetEventWldtEvent<?> physicalAssetEventWldtEvent) {
        logger.info("FunctionDrivenShadowing onPhysicalAssetEventNotification()");
    }

    @Override
    protected void onPhysicalAssetRelationshipEstablished(PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> physicalAssetRelationshipInstanceCreatedWldtEvent) {
        logger.info("FunctionDrivenShadowing onPhysicalAssetRelationshipEstablished()");
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> physicalAssetRelationshipInstanceDeletedWldtEvent) {
        logger.info("FunctionDrivenShadowing onPhysicalAssetRelationshipDeleted()");
    }

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> digitalActionWldtEvent) {

        logger.info("FunctionDrivenShadowing onDigitalActionEvent() -> ActionKey: " + digitalActionWldtEvent.getActionKey());

        final Timer timer = metricRegistry.timer(name(FunctionDrivenShadowing.class, "function-management"));
        final Timer.Context context = timer.time();

        try{

            if(digitalActionWldtEvent.getActionKey().equals(FunctionConfiguration.DT_FUNCTION_ACTION_KEY) && digitalActionWldtEvent.getBody() instanceof String){

                String pythonScriptBase64 = (String) digitalActionWldtEvent.getBody();

                //Save Python Script
                saveReceivedPythonScript(pythonScriptBase64);

                logger.info("Python Script Updated !");
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            context.stop();
        }
    }

    private void saveReceivedPythonScript(String pythonScriptBase64) {
        try{

            byte[] decodedBytes = Base64.getDecoder().decode(pythonScriptBase64);

            String outputFileName = "functions/target_shadowing_function.py";
            FileUtils.writeByteArrayToFile(new File(outputFileName), decodedBytes);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

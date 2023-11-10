package no.cantara.realestate.cloudconnector.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.stingray.security.application.StingrayAction;

import java.util.Random;


@Path("/systemstatus")
public class SystemStatusResource {

    private final Random random;

    public SystemStatusResource(Random random) {
        this.random = random;
    }

    @GET
    @Path("/gui")
    @Produces(MediaType.TEXT_PLAIN)
    @StingrayAction("statusgui")
    public Response getSystemStatusGui() {
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Message Flow Status</title>
                <style>
                  #messageFlow {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin: 0 auto;
                    max-width: 800px;
                  }
                                
                  .component {
                    border: 1px solid #333;
                    padding: 10px;
                    border-radius: 5px;
                    text-align: center;
                    flex: 1; /* Allows for equal width of components */
                  }
                                
                  .arrow {
                    margin: 0 10px;
                    font-size: 24px;
                  }
                                
                  .active {
                    background-color: #9f9;
                  }
                                
                  .idle {
                    background-color: #ff9;
                  }
                                
                  .error {
                    background-color: #f99;
                  }
                                
                  /* Additional styles for positioning and visual separation */
                  .component-group {
                    display: flex;
                    align-items: center;
                  }
                  .vertical-group {
                    display:flex;
                    flex-direction: column;
                  }
                                
                </style>
                </head>
                <body>
                  <h1>System Message Flow</h1>
                                
                  <div id="messageFlow">
                  <div class="vertical-group">
                    <div class="component-group">
                      <div id="PresentValueIngestion" class="component">Present Value Ingestion</div>
                      <div id="arrow1" class="arrow">✅</div>
                    </div>
                    <div class="component-group">
                      <div id="TrendObservationIngestion" class="component">Trend Observation Ingestion</div>
                      <div id="arrow2" class="arrow">✅</div>
                    </div>
                   \s
                  </div>
                   \s
                    <div id="MessageRouter" class="component">Message Router</div>
                   \s
                    <div class="component-group">
                      <div id="arrow3" class="arrow">✅</div>
                      <div id="MessageDistributor" class="component">Message Distributor</div>
                    </div>
                  </div>
                                
                  <script type="text/javascript">
                    // Dummy status data
                    const statuses = {
                      PresentValueIngestion: 'idle',
                      TrendObservationIngestion: 'active',
                      MessageRouter: 'error',
                      MessageDistributor: 'active'
                    };
                                
                    function updateComponentStatus(componentId, status) {
                      const component = document.getElementById(componentId);
                      component.className = 'component'; // Reset class
                      component.classList.add(status);
                                
                      // Update the text as well
                      const statusText = status.charAt(0).toUpperCase() + status.slice(1);
                      component.innerHTML = `${componentId.replace(/([A-Z])/g, ' $1').trim()} (Status: ${statusText})`;
                    }
                                
                    function updateArrow(arrowId, isConnected) {
                      const arrow = document.getElementById(arrowId);
                      arrow.textContent = isConnected ? '✅' : '❌';
                    }
                                
                    function updateStatus() {
                      updateComponentStatus('PresentValueIngestion', statuses.PresentValueIngestion);
                      updateComponentStatus('TrendObservationIngestion', statuses.TrendObservationIngestion);
                      updateComponentStatus('MessageRouter', statuses.MessageRouter);
                      updateComponentStatus('MessageDistributor', statuses.MessageDistributor);
                                
                      // Update arrows based on status
                      updateArrow('arrow1', statuses.PresentValueIngestion === 'active');
                      updateArrow('arrow2', statuses.TrendObservationIngestion === 'active');
                      updateArrow('arrow3', statuses.MessageRouter === 'active');
                    }
                                
                    // Update status on page load
                    updateStatus();
                  </script>
                </body>
                </html>
                """;
        return Response.ok(html).build();
    }
}

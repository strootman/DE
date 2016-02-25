package messaging

import (
	"encoding/json"
	"fmt"
	"logcabin"
	"model"
	"os"
	"reflect"
	"testing"

	"github.com/streadway/amqp"
)

var l = logcabin.New("test_amqp", "test_amqp")
var client *Client

func GetClient(t *testing.T) *Client {
	var err error
	if client != nil {
		return client
	}
	client, err = NewClient(uri(), false)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	client.SetupPublishing(JobsExchange)
	go client.Listen()
	return client
}

func shouldrun() bool {
	if os.Getenv("RUN_INTEGRATION_TESTS") != "" {
		return true
	}
	return false
}

func uri() string {
	return "amqp://guest:guest@rabbit:5672/"
}

func TestConstants(t *testing.T) {
	expected := 0
	actual := int(Launch)
	if actual != expected {
		t.Errorf("Launch was %d instead of %d", actual, expected)
	}
	expected = 1
	actual = int(Stop)
	if actual != expected {
		t.Errorf("Stop was %d instead of %d", actual, expected)
	}
	expected = 0
	actual = int(Success)
	if actual != expected {
		t.Errorf("Success was %d instead of %d", actual, expected)
	}
}

func TestNewStopRequest(t *testing.T) {
	actual := NewStopRequest()
	expected := &StopRequest{Version: 0}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("NewStopRequest returned:\n%#v\n\tinstead of:\n%#v", actual, expected)
	}
}

func TestNewLaunchRequest(t *testing.T) {
	job := &model.Job{}
	actual := NewLaunchRequest(job)
	expected := &JobRequest{
		Version: 0,
		Job:     job,
		Command: Launch,
	}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("NewLaunchRequest returned:\n%#v\n\tinstead of:\n%#v", actual, expected)
	}
}

func TestNewClient(t *testing.T) {
	if !shouldrun() {
		return
	}
	actual, err := NewClient(uri(), false)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	defer actual.Close()
	expected := uri()
	if actual.uri != expected {
		t.Errorf("Client's uri was %s instead of %s", actual.uri, expected)
	}
}

func TestClient(t *testing.T) {
	if !shouldrun() {
		return
	}

	client := GetClient(t)

	//defer client.Close()
	key := "tests"
	actual := ""
	expected := "this is a test"
	coord := make(chan int)

	handler := func(d amqp.Delivery) {
		d.Ack(false)
		actual = string(d.Body)
		coord <- 1
	}
	client.AddConsumer(JobsExchange, "test_queue", key, handler)
	client.Publish(key, []byte(expected))
	<-coord
	if actual != expected {
		t.Errorf("Handler received %s instead of %s", actual, expected)
	}

}

func TestSendTimeLimitRequest(t *testing.T) {
	if !shouldrun() {
		return
	}
	client := GetClient(t)
	var actual []byte
	coord := make(chan int)
	handler := func(d amqp.Delivery) {
		d.Ack(false)
		actual = d.Body
		coord <- 1
	}
	key := fmt.Sprintf("%s.%s", TimeLimitRequestsKey, "test")
	client.AddConsumer(JobsExchange, "test_queue1", key, handler)
	client.SendTimeLimitRequest("test")
	<-coord
	req := &TimeLimitRequest{}
	err := json.Unmarshal(actual, req)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if req.InvocationID != "test" {
		t.Errorf("TimeLimitRequest's InvocationID was %s instead of test", req.InvocationID)
	}
}

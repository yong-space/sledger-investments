import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { RecoilRoot } from 'recoil';
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import Portfolio from "./portfolio.jsx";
import StatusBar from "./statusbar.jsx";
import './index.css';

const darkTheme = createTheme({
  palette: { mode: "dark" },
});

const App = () => {
  return (
    <ThemeProvider theme={darkTheme}>
      <CssBaseline />
      <Portfolio />
      <StatusBar />
    </ThemeProvider>
  );
};

createRoot(document.querySelector("#root")).render(
  <StrictMode>
    <RecoilRoot>
      <App />
    </RecoilRoot>
  </StrictMode>
);
